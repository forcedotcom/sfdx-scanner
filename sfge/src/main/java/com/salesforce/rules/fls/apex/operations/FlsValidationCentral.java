package com.salesforce.rules.fls.apex.operations;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides mechanisms to build expected validations based on Dml operations and existing
 * validations based on validation operations. Dml operation formats are: insert accounts;
 * Database.insert(accounts);
 *
 * <p>Validation mechanisms supported are: Schema.SObjectType.Account.fields.isCreateable(); (and
 * other related formats) Security.stripInaccessible(AccessType.Create, accounts);
 */
public class FlsValidationCentral {
    private static final Logger LOGGER = LogManager.getLogger(FlsValidationCentral.class);
    private final boolean IS_WARNING_VIOLATION_DISABLED =
            SfgeConfigProvider.get().isWarningViolationDisabled();

    private final Set<FlsValidationRepresentation.Info> existingSchemaBasedValidations;
    private final Set<FlsViolationInfo> violations;
    private final FlsValidationType validationType;
    private final Multimap<FlsApexValueVertexWrapper, FlsValidationRepresentation>
            expectedReadValidations;
    private final Set<FlsValidationRepresentation> expectedValidations;

    public FlsValidationCentral(FlsValidationType validationType) {
        this.existingSchemaBasedValidations = new HashSet<>();
        this.violations = new HashSet<>();
        this.validationType = validationType;
        this.expectedValidations = new HashSet<>();
        this.expectedReadValidations = HashMultimap.create();
    }

    public Set<FlsViolationInfo> getViolations() {
        return this.violations;
    }

    /**
     * Checks if a given method has FLS validation that looks like Schema.SObjectType.blah. Also,
     * currently referred to as legacy FLS check.
     *
     * @param vertex method under scrutiny
     * @param symbols to provide context information
     */
    public void checkSchemaBasedFlsValidation(
            BaseSFVertex parent, BaseSFVertex vertex, SymbolProvider symbols) {
        final SchemaBasedValidationAnalyzer analyzer = new SchemaBasedValidationAnalyzer(symbols);
        final Set<FlsValidationRepresentation.Info> validationRepInfos =
                analyzer.checkForValidation(parent, vertex);
        this.existingSchemaBasedValidations.addAll(validationRepInfos);
    }

    /**
     * Checks if stripInaccessible() was invoked on a READ call. If found, matches against expected
     * validations and marks it as handled.
     */
    public void performStripInaccessibleValidationForRead(
            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (!FlsValidationType.READ.equals(validationType)) {
            throw new UnexpectedException(
                    "This method is only meant for Read operations. You've invoked it on "
                            + validationType);
        }

        final Optional<ApexValue<?>> readValueOptional =
                StripInaccessibleReadValidator.detectSanitization(vertex, symbols);
        if (readValueOptional.isPresent()) {
            final ApexValue<?> readValue = readValueOptional.get();
            final FlsApexValueVertexWrapper readValueWrapper =
                    new FlsApexValueVertexWrapper(readValue);
            final Collection<FlsValidationRepresentation> flsValidationRepresentationsForRead =
                    expectedReadValidations.get(readValueWrapper);

            if (!flsValidationRepresentationsForRead.isEmpty()) {
                // Values we found in the MultiMap are the FlsValidationReps for Read operation
                // that have been sanitized by stripInaccessible()
                // 1. Create Warning violations
                createStripInaccessibleWarningViolations(
                        readValue, flsValidationRepresentationsForRead);

                // 2. Remove the values from the map since they've been sanitized
                expectedReadValidations.removeAll(readValueWrapper);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Detected stripInaccessible {} for read operation {}",
                            vertex,
                            readValue);
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Did not find apexValue for vertex: " + vertex);
            }
        }
    }

    /** Creates warning violations for stripInaccessible check on Read. */
    private void createStripInaccessibleWarningViolations(
            ApexValue<?> apexValue, Collection<FlsValidationRepresentation> validationReps) {
        // If warnings are enabled, create stripInaccessible warnings
        if (!IS_WARNING_VIOLATION_DISABLED) {
            final Set<FlsViolationInfo> warningViolations =
                    FlsViolationCreatorUtil.createStripInaccessibleWarningViolations(
                            apexValue, validationReps);
            violations.addAll(warningViolations);
        }
    }

    /**
     * Creates expected validations based on a given DmlStatement such as: insert account; delete
     * contacts;
     *
     * @param vertex dml statement under scrutiny
     * @param symbols to provide context
     */
    public void createExpectedValidations(DmlStatementVertex vertex, SymbolProvider symbols) {
        // If the value has already been sanitized by StripInaccessible, there's nothing more to
        // check
        if (SanitizationChecker.checkSanitization(vertex, symbols)) {
            return;
        }

        final List<BaseSFVertex> dmlStatementVertexChildren = vertex.getChildren();
        if (dmlStatementVertexChildren.size() != validationType.parameterCount) {
            throw new UnexpectedException(
                    "Unexpected count of parameters: " + dmlStatementVertexChildren.size());
        }

        // Create expected validations based on the first parameter.
        // Even though MERGE operation takes two parameters, both of them need to be of the same
        // type.
        // The operation runs at an object level and we don't need to capture individual fields.
        // Hence, analyzing the first parameter works well.
        final BaseSFVertex childVertex = dmlStatementVertexChildren.get(0);

        final ValidationConverter validationConverter = new ValidationConverter(validationType);
        final Set<FlsValidationRepresentation> validationReps = new HashSet<>();
        if (childVertex instanceof ChainedVertex) {
            final Optional<ApexValue<?>> apexValue =
                    ScopeUtil.resolveToApexValue(symbols, (ChainedVertex) childVertex);
            if (apexValue.isPresent()) {
                validationReps.addAll(
                        validationConverter.convertToExpectedValidations(apexValue.get()));
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            "TODO: Apex value not detected for dml's child vertex: " + childVertex);
                    // TODO: add telemetry
                }
                violations.add(
                        FlsViolationCreatorUtil.createUnresolvedCrudFlsViolation(
                                validationType, vertex));
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("TODO: Child vertex of DML is not a chained vertex: " + childVertex);
                // TODO: add telemetry
            }
            violations.add(
                    FlsViolationCreatorUtil.createUnresolvedCrudFlsViolation(
                            validationType, vertex));
        }

        expectedValidations.addAll(validationReps);
    }

    /**
     * Creates expected validations based on a Database.dmlOperation() such as:
     * Database.insert(accounts); Database.update(contacts); It also internally adds the validations
     * thus created to its internal set.
     *
     * @param vertex method that performs the dml operation
     * @param symbols to provide context
     */
    public void createExpectedValidations(
            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final List<ChainedVertex> parameters = vertex.getParameters();

        if (parameters.size() > validationType.parameterCount + 1
                || parameters.size() < validationType.parameterCount) {
            // Parameter count doesn't match our expectations. Only the following formats are
            // supported
            // Database.insert(list)
            // Database.insert(list, boolean)

            return;
        }

        // If the value has already been sanitized by StripInaccessible, there's nothing more to
        // check
        if (SanitizationChecker.checkSanitization(vertex, symbols)) {
            return;
        }

        ChainedVertex parameter = parameters.get(0);
        final ValidationConverter validationConverter = new ValidationConverter(validationType);

        final Optional<ApexValue<?>> apexValueOptional =
                ScopeUtil.resolveToApexValue(symbols, parameter);

        if (!apexValueOptional.isPresent()) {
            // TODO: add telemetry on missing parameter type that we need to handle in future
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "Database operation method has a parameter of unexpected type: "
                                + parameter);
            }
            // Add a violation to let users know that SFGE cannot resolve the parameter in the DML
            // operation
            // and the onus of verifying its check is on them.
            violations.add(
                    FlsViolationCreatorUtil.createUnresolvedCrudFlsViolation(
                            validationType, vertex));

        } else {
            final ApexValue<?> apexValue = apexValueOptional.get();

            // Add them to our set of expected validations
            final Set<FlsValidationRepresentation> validationReps =
                    validationConverter.convertToExpectedValidations(apexValue);
            // Capture the vertex on which the operation is performed
            // We'll need to capture accounts in:
            // Database.insert(accounts);
            // List<Account> accounts = Database.query('SELECT Name from Account');
            if (FlsValidationType.READ.equals(validationType)) {
                addReadValidationReps(vertex, symbols, validationReps);
            } else {
                // for all other Database operations, use parameter passed as key
                expectedValidations.addAll(validationReps);
            }
        }
    }

    /**
     * Creates expected validations based on Soql queries.
     *
     * @param vertex soql query to examine
     * @param symbols to provide context
     */
    public void createExpectedValidations(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        if (!FlsValidationType.READ.equals(validationType)) {
            throw new UnexpectedException(
                    "Did not expect to see a Soql expression vertex validation from a non Read operation. vertex: "
                            + vertex);
        }

        final ValidationConverter converter = new ValidationConverter(this.validationType);
        final Set<FlsValidationRepresentation> validationReps = converter.convert(vertex);

        addReadValidationReps(vertex, symbols, validationReps);
    }

    private void addReadValidationReps(
            ChainedVertex vertex,
            SymbolProvider symbols,
            Set<FlsValidationRepresentation> validationReps) {
        final Optional<ApexValue<?>> readValueOptional =
                ScopeUtil.resolveToApexValue(symbols, vertex);
        if (readValueOptional.isPresent()) {
            for (FlsValidationRepresentation validationRep : validationReps) {
                final ApexValue<?> readValue = readValueOptional.get();
                expectedReadValidations.put(
                        new FlsApexValueVertexWrapper(readValue), validationRep);
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Did not find apex value to match " + vertex);
            }
            expectedValidations.addAll(validationReps);
        }
    }

    /**
     * Checks if all expected validations are handled in the existing validations. If an expected
     * validation is not handled, it creates a new Violation.
     *
     * @param vertex to provide information about where the violation occurs.
     */
    public void tallyValidations(SFVertex vertex) {
        final ValidationMatcher validationMatcher = new ValidationMatcher();
        validationMatcher.tallyValidations(vertex);
    }

    /**
     * Instance of this class operates on FlsValidationCentral at an instance level (and hence a
     * non-static class). It helps with tallying the validations across overall expected
     * validations, existing schema-based validations, and existing stripInaccessible-based
     * validations.
     */
    private class ValidationMatcher {

        public void tallyValidations(SFVertex vertex) {
            matchSchemaBasedValidations(vertex);
        }

        private void matchSchemaBasedValidations(SFVertex vertex) {
            // Match with schema-based existing validations.
            // To do this match, we don't care about the individual vertices anymore. So let's get
            // the validations in the same bucket

            Set<FlsValidationRepresentation> expectedValidationsToAnalyze =
                    Sets.newHashSet(expectedValidations);
            expectedValidationsToAnalyze.addAll(expectedReadValidations.values());
            Set<FlsValidationRepresentation> analyzedExpectedValidations = new HashSet<>();

            expectedValidationsToAnalyze.forEach(
                    validationRep -> {
                        final Set<FlsViolationInfo> flsViolationInfos =
                                validationRep.compareWithExistingValidation(
                                        existingSchemaBasedValidations);
                        if (!flsViolationInfos.isEmpty()) {
                            for (FlsViolationInfo flsViolationInfo : flsViolationInfos) {
                                flsViolationInfo.setSinkVertex(vertex);
                            }
                            violations.addAll(flsViolationInfos);
                        } else {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(
                                        "Found matching validation for: "
                                                + validationRep.getValidationInfo());
                            }
                        }
                        analyzedExpectedValidations.add(validationRep);
                    });

            // Remove expected validations that have already been analyzed
            expectedValidations.removeIf(value -> analyzedExpectedValidations.contains(value));
        }
    }
}

package com.salesforce.graph.symbols.apex;

import com.salesforce.apex.ApexEnum;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.DefaultNoOpScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.FieldSet;
import com.salesforce.graph.symbols.apex.schema.FieldSetMember;
import com.salesforce.graph.symbols.apex.schema.SObjectField;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.symbols.apex.system.SObjectAccessDecision;
import com.salesforce.graph.vertex.BinaryExpressionVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewKeyValueObjectExpressionVertex;
import com.salesforce.graph.vertex.NewListInitExpressionVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewMapInitExpressionVertex;
import com.salesforce.graph.vertex.NewMapLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewSetInitExpressionVertex;
import com.salesforce.graph.vertex.NewSetLiteralExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.TernaryExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates all of the logic for creating ApexValues. It provides methods when the
 * caller knows the specific type to create and methods that determine the correct type based on
 * which values were passed to the builder.
 */
@SuppressWarnings(
        "PMD.AvoidFieldNameMatchingMethodName") // TODO: this is too widely used. Revisit in future
public class ApexValueBuilder implements DeepCloneable<ApexValueBuilder> {
    private static final Logger LOGGER = LogManager.getLogger(ApexValueBuilder.class);

    // This is the default since it is the most common.
    // TODO: Consider setting to null and require all callers to decide on the correct status.
    private static final ValueStatus DEFAULT_STATUS = ValueStatus.INITIALIZED;
    private static final String USE_NO_ARGS_VERSION = "Use no-args version";
    private static final String USE_SINGLE_ARGS_VERSION = "Use single-args version";

    private final SymbolProvider symbolProvider;
    private ValueStatus status;
    private HashSet<Constraint> positiveConstraints;
    private HashSet<Constraint> negativeConstraints;
    private ApexValue<?> returnedFrom;
    private Typeable declarationVertex;
    private ChainedVertex valueVertex;
    private InvocableVertex invocable;
    private MethodVertex method;
    private String methodReturnType;

    /**
     * Used to track previously built values, this avoids accidentally mixing values from different
     * builders. All public builder methods should pass the value through the either {@link
     * #registerResult(ApexValue)} or {@link #registerResult(Optional)} in order to keep track of
     * what was returned..
     */
    private Optional<ApexValue<?>> builtValue;

    private ApexValueBuilder(SymbolProvider symbolProvider) {
        this.symbolProvider = symbolProvider;
        this.status = DEFAULT_STATUS;
        this.positiveConstraints = new HashSet<>();
        this.negativeConstraints = new HashSet<>();
    }

    private ApexValueBuilder(ApexValueBuilder other) {
        other.verifySingleUse();
        this.symbolProvider = other.symbolProvider;
        this.status = other.status;
        this.returnedFrom = other.returnedFrom;
        this.declarationVertex = other.declarationVertex;
        this.valueVertex = other.valueVertex;
        this.positiveConstraints = CloneUtil.cloneHashSet(other.positiveConstraints);
        this.negativeConstraints = CloneUtil.cloneHashSet(other.negativeConstraints);
        this.invocable = other.invocable;
        this.method = other.method;
        this.methodReturnType = other.methodReturnType;
    }

    @Override
    public ApexValueBuilder deepClone() {
        return new ApexValueBuilder(this);
    }

    public static ApexValueBuilder getWithoutSymbolProvider() {
        return get(DefaultNoOpScope.getInstance());
    }

    public static ApexValueBuilder get(SymbolProvider symbolProvider) {
        if (symbolProvider == null) {
            throw new UnexpectedException("Symbol provider is required");
        }
        return new ApexValueBuilder(symbolProvider);
    }

    private void verifySingleUse() {
        if (builtValue != null) {
            throw new UnexpectedException(
                    "Builder should be cloned before building any values. builtValue="
                            + builtValue);
        }
    }

    private <T extends ApexValue<?>> T registerResult(T value) {
        verifySingleUse();
        builtValue = Optional.ofNullable(value);
        return value;
    }

    private <T extends ApexValue<?>> Optional<T> registerResult(Optional<T> value) {
        verifySingleUse();
        builtValue = (Optional<ApexValue<?>>) value;
        return value;
    }

    /**
     * Helper that calls {@link #buildOptional()} and defaults to an {@link ApexSingleValue} if
     * nothing more specific can be built.
     */
    public ApexValue<?> build() {
        verifySingleUse();
        checkValidity();
        // buildCorrectType and buildSingleValue both register the value. We don't need to register
        // it
        return buildCorrectType().orElseGet(() -> buildSingleValue());
    }

    /**
     * Build the most specific ApexValue except an ApexSingleValue. // TODO: Evaluate removing this
     * method and always returning an ApexSingleValue
     */
    public Optional<ApexValue<?>> buildOptional() {
        verifySingleUse();
        checkValidity();
        // buildCorrectType registers the value. We don't need to register it
        return buildCorrectType();
    }

    public ApexBooleanValue buildBoolean() {
        // Default uninitialized values to null
        Boolean value = null;
        if (valueVertex instanceof LiteralExpressionVertex.True) {
            value = true;
        } else if (valueVertex instanceof LiteralExpressionVertex.False) {
            value = false;
        }
        return registerResult(new ApexBooleanValue(value, this));
    }

    /**
     * WARNING: Using this method incorrectly can lead to methods being incorrectly excluded. This
     * method should only be used when {@code value} has valid meaning. Use {@link
     * #withStatus(ValueStatus)} if you need an indeterminant value.
     */
    public ApexBooleanValue buildBoolean(Boolean value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexBooleanValue(value, this));
    }

    public ApexIdValue buildId() {
        return buildId(new ApexIdValue(this));
    }

    public ApexIdValue buildId(ApexValue<?> apexValue) {
        return registerResult(new ApexIdValue(apexValue, this));
    }

    public ApexCustomValue buildCustomValue(Typeable typeable) {
        return registerResult(new ApexCustomValue(typeable, this));
    }

    public SObjectAccessDecision buildSObjectAccessDecision(
            ApexEnumValue accessTypeEnumValue, AbstractSanitizableValue sanitizableValue) {
        return registerResult(
                new SObjectAccessDecision(accessTypeEnumValue, sanitizableValue, this));
    }

    public ApexEnumValue buildEnum(ApexEnum apexEnum) {
        return registerResult(new ApexEnumValue(apexEnum, this));
    }

    public ApexEnumValue buildEnum(ApexEnum apexEnum, String value) {
        if (value == null) {
            throw new UnexpectedException(USE_SINGLE_ARGS_VERSION);
        }
        return buildEnum(apexEnum, apexEnum.getValue(value));
    }

    public ApexEnumValue buildEnum(ApexEnum apexEnum, ApexEnum.Value value) {
        if (value == null) {
            throw new UnexpectedException(USE_SINGLE_ARGS_VERSION);
        }
        return registerResult(new ApexEnumValue(apexEnum, value, this));
    }

    public ApexFieldDescribeMapValue buildApexFieldDescribeMapValue(SObjectType sObjectType) {
        return registerResult(new ApexFieldDescribeMapValue(sObjectType, this));
    }

    public ApexFieldSetDescribeMapValue buildApexFieldSetDescribeMapValue(SObjectType sObjectType) {
        return registerResult(new ApexFieldSetDescribeMapValue(sObjectType, this));
    }

    public ApexGlobalDescribeMapValue buildApexGlobalDescribeMap() {
        return registerResult(new ApexGlobalDescribeMapValue(this));
    }

    public FieldSet buildFieldSet(SObjectType sObjectType, ApexValue<?> fieldSetName) {
        if (fieldSetName instanceof ApexStringValue) {
            return registerResult(new FieldSet(sObjectType, (ApexStringValue) fieldSetName, this));
        } else if (fieldSetName instanceof ApexForLoopValue) {
            return registerResult(new FieldSet(sObjectType, (ApexForLoopValue) fieldSetName, this));
        } else if (fieldSetName instanceof ApexSingleValue) {
            return registerResult(new FieldSet(sObjectType, (ApexSingleValue) fieldSetName, this));
        } else {
            throw new UnexpectedException(sObjectType);
        }
    }

    public ApexFieldSetListValue buildFieldSetList(FieldSet fieldSet) {
        return new ApexFieldSetListValue(fieldSet, this);
    }

    public FieldSetMember buildFieldSetMember(FieldSet fieldSet) {
        return new FieldSetMember(fieldSet, this);
    }

    public ApexListValue buildList() {
        return registerResult(new ApexListValue(this));
    }

    public ApexMapValue buildMap() {
        return registerResult(new ApexMapValue(this));
    }

    private ApexSingleValue buildSingleValue() {
        return registerResult(new ApexSingleValue(this));
    }

    public ApexClassInstanceValue buildApexClassInstanceValue(
            AbstractClassInstanceScope classScope) {
        return registerResult(new ApexClassInstanceValue(classScope, this));
    }

    public DescribeFieldResult buildDescribeFieldResult() {
        return registerResult(new DescribeFieldResult(this));
    }

    public DescribeFieldResult buildDescribeFieldResult(
            DescribeSObjectResult describeSObjectResult, ApexValue<?> fieldName) {
        if (fieldName instanceof ApexStringValue) {
            return registerResult(
                    new DescribeFieldResult(
                            describeSObjectResult, (ApexStringValue) fieldName, this));
        } else if (fieldName instanceof ApexForLoopValue) {
            return registerResult(
                    new DescribeFieldResult(
                            describeSObjectResult, (ApexForLoopValue) fieldName, this));
        } else if (fieldName instanceof ApexSingleValue) {
            return registerResult(
                    new DescribeFieldResult(
                            describeSObjectResult, (ApexSingleValue) fieldName, this));
        } else {
            throw new UnexpectedException(fieldName);
        }
    }

    public DescribeSObjectResult buildDescribeSObjectResult(SObjectType sObjectType) {
        return registerResult(new DescribeSObjectResult(sObjectType, this));
    }

    public DescribeSObjectResult buildDescribeSObjectResult() {
        return registerResult(new DescribeSObjectResult(this));
    }

    public ApexForLoopValue buildForLoopValue() {
        return registerResult(new ApexForLoopValue(this));
    }

    /**
     * Build an ApexForLoopValue for the list. Copies over the valueVertex and delcarationVertex
     * from the original list.
     */
    public ApexForLoopValue buildForLoopValue(ApexListValue apexListValue) {
        this.valueVertex = apexListValue.getValueVertex().orElse(null);
        this.declarationVertex = apexListValue.getDeclarationVertex().orElse(null);
        return registerResult(new ApexForLoopValue(apexListValue, this));
    }

    public ApexDecimalValue buildDecimal() {
        // Default uninitialized values to null
        BigDecimal value = null;
        if (valueVertex instanceof LiteralExpressionVertex.Decimal) {
            value = ((LiteralExpressionVertex.Decimal) valueVertex).getLiteral();
        }
        return registerResult(new ApexDecimalValue(value, this));
    }

    public ApexDecimalValue buildDecimal(BigDecimal value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexDecimalValue(value, this));
    }

    public ApexDoubleValue buildDouble() {
        // Default uninitialized values to null
        Double value = null;
        if (valueVertex instanceof LiteralExpressionVertex.Double) {
            value = ((LiteralExpressionVertex.Double) valueVertex).getLiteral();
        }
        return registerResult(new ApexDoubleValue(value, this));
    }

    public ApexDoubleValue buildDouble(Double value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexDoubleValue(value, this));
    }

    public ApexIntegerValue buildInteger() {
        // Default uninitialized values to null
        Integer value = null;
        if (valueVertex instanceof LiteralExpressionVertex.Integer) {
            value = ((LiteralExpressionVertex.Integer) valueVertex).getLiteral();
        }
        return registerResult(new ApexIntegerValue(value, this));
    }

    public ApexIntegerValue buildInteger(Integer value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexIntegerValue(value, this));
    }

    public ApexLongValue buildLong() {
        // Default uninitialized values to null
        Long value = null;
        if (valueVertex instanceof LiteralExpressionVertex.Long) {
            value = ((LiteralExpressionVertex.Long) valueVertex).getLiteral();
        }
        return registerResult(new ApexLongValue(value, this));
    }

    public ApexLongValue buildLong(Long value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexLongValue(value, this));
    }

    public ApexSetValue buildSet() {
        return registerResult(new ApexSetValue(this));
    }

    public SObjectType buildSObjectType() {
        return registerResult(new SObjectType(this));
    }

    public SObjectType buildSObjectType(ApexValue<?> sObjectType) {
        if (sObjectType instanceof SObjectType) {
            throw new UnexpectedException(
                    "Use deepCloneForReturnInstead. sObjectType=" + sObjectType);
        }
        return registerResult(new SObjectType(sObjectType, this));
    }

    public SObjectType buildSObjectType(String associatedObjectType) {
        return registerResult(new SObjectType(associatedObjectType, this));
    }

    public SObjectAccessDecision buildSObjectAccessDecision() {
        return registerResult(new SObjectAccessDecision(this));
    }

    public SObjectField buildSObjectField(SObjectType sObjectType, ApexValue<?> fieldName) {
        if (fieldName instanceof ApexStringValue) {
            return registerResult(new SObjectField(sObjectType, (ApexStringValue) fieldName, this));
        } else if (fieldName instanceof ApexForLoopValue) {
            return registerResult(
                    new SObjectField(sObjectType, (ApexForLoopValue) fieldName, this));
        } else if (fieldName instanceof ApexSingleValue) {
            return registerResult(new SObjectField(sObjectType, (ApexSingleValue) fieldName, this));
        } else {
            throw new UnexpectedException(
                    "sObjectType=" + sObjectType + ", fieldName=" + fieldName);
        }
    }

    public ApexSingleValue buildSObjectInstance(Typeable typeable) {
        // TODO: Consider ApexSObject class
        return registerResult(new ApexSingleValue(typeable, this));
    }

    public ApexSoqlValue buildSoql() {
        return registerResult(new ApexSoqlValue(this));
    }

    public ApexStringValue buildString(String value) {
        if (value == null) {
            throw new UnexpectedException(USE_NO_ARGS_VERSION);
        }
        this.status = ValueStatus.INITIALIZED;
        return registerResult(new ApexStringValue(value, this));
    }

    public ApexStringValue buildString() {
        String value = null;
        if (valueVertex instanceof LiteralExpressionVertex.SFString) {
            LiteralExpressionVertex.SFString vertex =
                    (LiteralExpressionVertex.SFString) valueVertex;
            value = vertex.getLiteral();
        }
        return registerResult(new ApexStringValue(value, this));
    }

    public ApexSingleValue buildUnknownType() {
        return registerResult(new ApexSingleValue(this));
    }

    public ApexValueBuilder declarationVertex(Typeable declarationVertex) {
        this.declarationVertex = declarationVertex;
        return this;
    }

    public ApexValueBuilder valueVertex(ChainedVertex valueVertex) {
        if (valueVertex instanceof TernaryExpressionVertex) {
            // TODO: Hack hack hack. TODO: These should be separate paths, or similar to a ForLoop
            // object
            TernaryExpressionVertex ternaryExpression = (TernaryExpressionVertex) valueVertex;
            this.valueVertex = ternaryExpression.getTrueValue();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "TODO: Choosing trueValue. ternaryVertex="
                                + valueVertex
                                + ", trueValue="
                                + ternaryExpression.getTrueValue()
                                + ", falseValue="
                                + ternaryExpression.getFalseValue());
            }
        } else {
            this.valueVertex = valueVertex;
        }
        return this;
    }

    // Force the caller to consider whether to set both since they are often set together
    public ApexValueBuilder returnedFrom(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        this.returnedFrom = returnedFrom;
        this.invocable = invocable;
        return this;
    }

    public ApexValueBuilder methodVertex(MethodVertex method) {
        this.method = method;
        return this;
    }

    public ApexValueBuilder methodReturnType(String methodReturnType) {
        this.methodReturnType = methodReturnType;
        return this;
    }

    /** Set the status, {@link #DEFAULT_STATUS} is the default if this method is never invoked. */
    public ApexValueBuilder withStatus(ValueStatus status) {
        this.status = status;
        return this;
    }

    /** Add a positive {@link Constraint} on the ApexValue */
    public ApexValueBuilder addPositiveConstraint(Constraint constraint) {
        positiveConstraints.add(constraint);
        return this;
    }

    /** Add a negative {@link Constraint} on the ApexValue */
    public ApexValueBuilder addNegativeConstraint(Constraint constraint) {
        negativeConstraints.add(constraint);
        return this;
    }

    public @Nullable Typeable getDeclarationVertex() {
        return declarationVertex;
    }

    /**
     * @return the most specific type that can be determined based on the values supplied to the
     *     builder. The order of precedence is {@link #valueVertex}, {@link #declarationVertex},
     *     then {@link #methodReturnType}.
     */
    public Optional<Typeable> getMostSpecificTypedVertex() {
        if (valueVertex instanceof Typeable
                && !(valueVertex instanceof LiteralExpressionVertex.Null)) {
            return Optional.of((Typeable) valueVertex);
        } else if (declarationVertex != null) {
            return Optional.of(declarationVertex);
        } else if (methodReturnType != null) {
            return Optional.of(SyntheticTypedVertex.get(methodReturnType));
        } else {
            return Optional.empty();
        }
    }

    public InvocableVertex getInvocable() {
        return invocable;
    }

    public MethodVertex getMethod() {
        return method;
    }

    public ApexValue<?> getReturnedFrom() {
        return returnedFrom;
    }

    public SymbolProvider getSymbolProvider() {
        return symbolProvider;
    }

    public ChainedVertex getValueVertex() {
        return valueVertex;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public HashSet<Constraint> getPositiveConstraints() {
        return this.positiveConstraints;
    }

    public HashSet<Constraint> getNegativeConstraints() {
        return this.negativeConstraints;
    }

    /**
     * Uses the ValueVertex to determine the correct type to create. This is used when the caller
     * doesn't know or care which type will be created. The method uses the {@link #valueVertex} but
     * falls back to the {@link #declarationVertex} if the type can't be determined from the
     * valueVertex.
     */
    private Optional<ApexValue<?>> buildCorrectType() {
        checkValidity();
        ApexValue<?> result = buildCorrectTypeFromValue();

        if (result == null) {
            result = buildCorrectTypeFromDeclaration();
        }

        if (result == null) {
            // deepClone since buildCorrectTypeFromMethodReturnType may not succeed and this builder
            // could be used for
            // another purpose
            result =
                    deepClone()
                            .withStatus(ValueStatus.INDETERMINANT)
                            .buildCorrectTypeFromMethodReturnType();
        }

        return Optional.ofNullable(result);
    }

    // Any values here should be added to buildCorrectTypeFromValue
    // TODO: Unify
    private final TreeMap<String, Supplier<ApexValue<?>>> typeToBuilderSuppliers =
            CollectionUtil.newTreeMapOf(
                    Pair.of(ApexBooleanValue.TYPE, this::buildBoolean),
                    Pair.of(ApexDoubleValue.TYPE, this::buildDouble),
                    Pair.of(ApexDecimalValue.TYPE, this::buildDecimal),
                    Pair.of(ApexIntegerValue.TYPE, this::buildInteger),
                    Pair.of(ApexLongValue.TYPE, this::buildLong),
                    Pair.of(ApexStringValue.TYPE, this::buildString),
                    Pair.of(ApexIdValue.TYPE, this::buildId),
                    Pair.of(DescribeSObjectResult.TYPE, this::buildDescribeSObjectResult),
                    Pair.of(SObjectAccessDecision.TYPE, this::buildSObjectAccessDecision),
                    Pair.of(SObjectType.TYPE, this::buildSObjectType));

    /** Uses the declaration vertex to build the correct type. */
    private @Nullable ApexValue<?> buildCorrectTypeFromDeclaration() {
        if (declarationVertex == null) {
            return null;
        }

        String type =
                ApexStandardLibraryUtil.getCanonicalName(declarationVertex.getCanonicalType());
        return buildCorrectTypeFromType(type);
    }

    // TODO: Consider annotating the value so that these values can be ranked lower when collapsing
    // paths
    private @Nullable ApexValue<?> buildCorrectTypeFromMethodReturnType() {
        if (methodReturnType == null) {
            return null;
        }

        if (methodReturnType.equalsIgnoreCase(ASTConstants.TYPE_VOID)) {
            throw new UnexpectedException("This should not be used on void methods");
        }

        return buildCorrectTypeFromType(methodReturnType);
    }

    private @Nullable ApexValue<?> buildCorrectTypeFromType(String typeParam) {
        final String type = typeParam.toLowerCase(Locale.ROOT);

        if (type.startsWith(ASTConstants.TypePrefix.LIST)) {
            return buildList();
        } else if (type.startsWith(ASTConstants.TypePrefix.MAP)) {
            return buildMap();
        } else if (type.startsWith(ASTConstants.TypePrefix.SET)) {
            return buildSet();
        } else if (typeToBuilderSuppliers.containsKey(type)) {
            return typeToBuilderSuppliers.get(type).get();
        } else {
            ApexEnum apexEnum = MetadataInfoProvider.get().getEnum(type).orElse(null);
            if (apexEnum != null) {
                return buildEnum(apexEnum);
            }
        }

        return null;
    }

    /** Uses the value vertex to build the correct type. */
    private @Nullable ApexValue<?> buildCorrectTypeFromValue() {
        ApexValue<?> result = null;

        // Any items added here should also be added to typeToBuilderSuppliers
        // TODO: Unify

        if (valueVertex instanceof VariableExpressionVertex.Standard) {
            result = ((VariableExpressionVertex.Standard) valueVertex).getApexValue().deepClone();
        } else if (valueVertex instanceof LiteralExpressionVertex.Decimal) {
            this.status = ValueStatus.INITIALIZED;
            result = buildDecimal();
        } else if (valueVertex instanceof LiteralExpressionVertex.Double) {
            this.status = ValueStatus.INITIALIZED;
            result = buildDouble();
        } else if (valueVertex instanceof LiteralExpressionVertex.False) {
            this.status = ValueStatus.INITIALIZED;
            result = buildBoolean();
        } else if (valueVertex instanceof LiteralExpressionVertex.Integer) {
            this.status = ValueStatus.INITIALIZED;
            result = buildInteger();
        } else if (valueVertex instanceof LiteralExpressionVertex.Long) {
            this.status = ValueStatus.INITIALIZED;
            result = buildLong();
        } else if (valueVertex instanceof LiteralExpressionVertex.Null) {
            this.status = ValueStatus.INITIALIZED;
            if (declarationVertex != null) {
                // Try to initialize the value using the declaration vertex since the null vertex
                // doesn't contain type information
                result = buildCorrectTypeFromDeclaration();
            }
            if (result == null) {
                // Try to initialize the value using the return type vertex since the null vertex
                // doesn't contain type information
                result = buildCorrectTypeFromMethodReturnType();
            }
            if (result == null) {
                result = buildSingleValue();
            }
        } else if (valueVertex instanceof LiteralExpressionVertex.SFString) {
            this.status = ValueStatus.INITIALIZED;
            result = buildString();
        } else if (valueVertex instanceof LiteralExpressionVertex.True) {
            this.status = ValueStatus.INITIALIZED;
            result = buildBoolean();
        } else if (valueVertex instanceof LiteralExpressionVertex) {
            this.status = ValueStatus.INITIALIZED;
            result = buildSingleValue();
        } else if (valueVertex instanceof BooleanExpressionVertex) {
            result = buildBoolean();
        } else if (valueVertex instanceof BinaryExpressionVertex) {
            result =
                    ApexValueUtil.applyBinaryExpression(
                                    (BinaryExpressionVertex) valueVertex, symbolProvider)
                            .orElse(null);
        } else if (valueVertex instanceof VariableExpressionVertex.Single) {
            // Try to resolve a Single variable to an ApexValue
            result =
                    symbolProvider
                            .getApexValue((VariableExpressionVertex) valueVertex)
                            .orElse(null);
        } else if (valueVertex instanceof VariableExpressionVertex.ForLoop) {
            return buildForLoopValue();
        } else if (valueVertex instanceof VariableExpressionVertex) {
            // Any other variable types are converted to a generic ApexSingleValue
            result = buildSingleValue();
        } else if (valueVertex instanceof NewListInitExpressionVertex
                || valueVertex instanceof NewListLiteralExpressionVertex) {
            this.status = ValueStatus.INITIALIZED;
            result = buildList();
        } else if (valueVertex instanceof NewKeyValueObjectExpressionVertex) {
            this.status = ValueStatus.INITIALIZED;
            NewKeyValueObjectExpressionVertex kvVertex =
                    (NewKeyValueObjectExpressionVertex) valueVertex;
            if (TypeableUtil.isCustomSetting(kvVertex.getCanonicalType())) {
                result = buildCustomValue(kvVertex);
            } else {
                // TODO: Should this be an ApexMapValue?
                result = buildSingleValue();
            }
        } else if (valueVertex instanceof NewMapInitExpressionVertex
                || valueVertex instanceof NewMapLiteralExpressionVertex) {
            this.status = ValueStatus.INITIALIZED;
            result = buildMap();
        } else if (valueVertex instanceof NewSetInitExpressionVertex
                || valueVertex instanceof NewSetLiteralExpressionVertex) {
            this.status = ValueStatus.INITIALIZED;
            result = buildSet();
        } else if (valueVertex instanceof SoqlExpressionVertex) {
            // A Soql query can never return Null
            addNegativeConstraint(Constraint.Null);
            // Soql expressions are are turned into a List with a single ApexSoqlValue, or a single
            // ApexSoqlValue
            // depending on what the SOQL query is assigned to
            // TODO: This seems to lose information. ApexListValue does not have all of the same
            // methods as ApexSoqlValue
            if (declarationVertex != null
                    && declarationVertex
                            .getCanonicalType()
                            .toLowerCase(Locale.ROOT)
                            .startsWith(ASTConstants.TypePrefix.LIST)) {
                result = buildList();
            } else {
                result = buildSoql();
            }
        }

        // Attempt to resolve the value to something that was returned from a method call if we have
        // made it this far
        // without a result and the valueVertex is an InvocableWithParametersVertex,
        if (result == null && valueVertex instanceof InvocableWithParametersVertex) {
            // TODO: This seems error prone. It is ignoring all other items passed into the
            // ApexValueBuilder. For now
            // we will log when the statuses don't match.
            result =
                    symbolProvider
                            .getReturnedValue((InvocableWithParametersVertex) valueVertex)
                            .orElse(null);
            if (result != null) {
                if (!status.equals(result.getStatus())) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Mismatched status. result=" + result + ", this=" + this);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "ApexValueBuilder{"
                + "status="
                + status
                + ", positiveConstraints="
                + positiveConstraints
                + ", negativeConstraints="
                + negativeConstraints
                + ", returnedFrom="
                + returnedFrom
                + ", declarationVertex="
                + declarationVertex
                + ", valueVertex="
                + valueVertex
                + ", invocable="
                + invocable
                + ", method="
                + method
                + ", methodReturnType='"
                + methodReturnType
                + '\''
                + '}';
    }

    private void checkValidity() {
        if (declarationVertex == null && valueVertex == null) {
            throw new UnexpectedException("Declaration or Value must be provided");
        }

        if (status == null) {
            throw new UnexpectedException("Status is required");
        }
    }
}

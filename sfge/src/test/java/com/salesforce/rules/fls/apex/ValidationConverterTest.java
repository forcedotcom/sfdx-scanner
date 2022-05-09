package com.salesforce.rules.fls.apex;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.DmlUpsertStatementVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsValidationRepresentation;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.ValidationConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ValidationConverterTest {
    private static final Logger LOGGER = LogManager.getLogger(ValidationConverterTest.class);
    static final FlsValidationType INSERT = FlsValidationType.INSERT;

    private GraphTraversalSource g;

    @RegisterExtension
    public BeforeEachCallback watcher =
            context -> LOGGER.info("Starting test: " + context.getTestMethod().get().getName());

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testValidation_objectTypeDeclared_SingleItem_objProperty() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + "       insert accounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_objectTypeDeclared_SingleItem_keyValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       accounts.add(a);\n"
                        + "       insert accounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_objectTypeDeclared_MultipleItems_keyValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       Account acc2 = new Account(Phone = '123-456-7890');\n"
                        + "       accounts.add(acc1);\n"
                        + "       accounts.add(acc2);\n"
                        + "       insert accounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");
        expectedValidation.addField("Phone");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_noObjectType_SingleItem_keyValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       objects.add(acc1);\n"
                        + "       insert objects;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_noObjectType_MultipleItems_keyValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> objects = new List<SObject>();\n"
                        + "       Account acc1 = new Account(Name = 'foo1');\n"
                        + "       Contact con1 = new Contact(FirstName = 'Jane', LastName = 'Doe');\n"
                        + "       objects.add(acc1);\n"
                        + "       objects.add(con1);"
                        + "       insert objects;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation1 = new FlsValidationRepresentation();
        expectedValidation1.setValidationType(INSERT);
        expectedValidation1.setObject("Account");
        expectedValidation1.addField("name");

        final FlsValidationRepresentation expectedValidation2 = new FlsValidationRepresentation();
        expectedValidation2.setValidationType(INSERT);
        expectedValidation2.setObject("Contact");
        expectedValidation2.addField("firstname");
        expectedValidation2.addField("lastname");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos1 =
                expectedValidation1.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos1.isEmpty());

        final Set<FlsViolationInfo> flsViolationInfos2 =
                expectedValidation2.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos2.isEmpty());
    }

    @Test
    public void testValidation_objectTypeDeclared_MultipleItemsFromQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = [SELECT Name, Phone from Account];\n"
                        + "       List<Account> newAccounts = new List<Account>();\n"
                        + "       for (Account acc: accounts) {\n"
                        + "           Account a = new Account(Name = acc.name, Phone = acc.phone);\n"
                        + "           newAccounts.add(a);\n"
                        + "       }\n"
                        + "       insert newAccounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");
        expectedValidation.addField("Phone");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_objectTypeDeclared_MultipleItemsFromDirectQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "        List<Account> newAccounts = new List<Account>();\n"
                        + "        for (Account acc: [SELECT Name, Phone from Account]) {\n"
                        + "            Account a = new Account(Name = acc.name, Phone = acc.phone);\n"
                        + "            newAccounts.add(a);\n"
                        + "        }\n"
                        + "        insert newAccounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(INSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");
        expectedValidation.addField("Phone");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testValidation_singleUpsert() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Account a = new Account();\n"
                        + "       a.name = 'Acme Inc.';\n"
                        + "       accounts.add(a);\n"
                        + "       upsert accounts;\n"
                        + "   }\n"
                        + "}\n";

        final FlsValidationRepresentation expectedValidation = new FlsValidationRepresentation();
        expectedValidation.setValidationType(FlsValidationType.UPSERT);
        expectedValidation.setObject("Account");
        expectedValidation.addField("name");

        List<FlsValidationRepresentation.Info> validations = getActualValidations(sourceCode);

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedValidation.compareWithExistingValidation(validations);
        assertTrue(flsViolationInfos.isEmpty());
    }

    private List<FlsValidationRepresentation.Info> getActualValidations(String sourceCode) {
        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "foo");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);

        List<FlsValidationRepresentation> validationReps = new ArrayList<>();

        DefaultNoOpPathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        return visit((DmlStatementVertex) vertex, symbols);
                    }

                    @Override
                    public boolean visit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {
                        return visit((DmlStatementVertex) vertex, symbols);
                    }

                    public boolean visit(DmlStatementVertex vertex, SymbolProvider symbols) {
                        VariableExpressionVertex childVertex = vertex.getChild(0);

                        ValidationConverter converter =
                                new ValidationConverter(FlsValidationType.UPSERT);
                        validationReps.addAll(
                                converter.convertToExpectedValidations(
                                        ScopeUtil.resolveToApexValue(symbols, childVertex).get()));
                        return true;
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        List<FlsValidationRepresentation.Info> validations = new ArrayList<>();
        validationReps.forEach(
                wrapper -> {
                    validations.addAll(wrapper.getValidationInfo());
                });
        return validations;
    }
}

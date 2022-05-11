package com.salesforce.graph.symbols.apex;

import static com.salesforce.matchers.OptionalMatcher.optEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.system.SObjectAccessDecision;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SObjectAccessDecisionTest {
    static final FlsConstants.StripInaccessibleAccessType CREATEABLE_ACCESS_TYPE =
            FlsConstants.StripInaccessibleAccessType.CREATABLE;
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testAccessDecisionValueCreation() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		System.debug(sd.getRecords());\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(
                outputListValue.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));

        final Typeable typeable = outputListValue.getListType().get();
        assertThat(typeable.getCanonicalType(), equalToIgnoringCase("Account"));
        final ApexSingleValue firstItem = (ApexSingleValue) outputListValue.get(0);
        assertThat(firstItem.isIndeterminant(), equalTo(true));
        assertThat(
                firstItem.getTypeVertex().get().getCanonicalType(), equalToIgnoringCase("Account"));
    }

    @Test
    public void testAccessDecisionValueIncorrectAccessType() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		System.debug(sd.getRecords());\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(
                outputListValue.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        FlsConstants.StripInaccessibleAccessType.UPDATABLE),
                equalTo(false));
    }

    @Test
    public void testAccessDecisionValueToTypedList() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		List<Account> sanitizedValues = sd.getRecords();\n"
                    + "		System.debug(sanitizedValues);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(
                outputListValue.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));

        final Typeable typeable = outputListValue.getListType().get();
        assertThat(typeable.getCanonicalType(), equalToIgnoringCase("Account"));
        final ApexSingleValue firstItem = (ApexSingleValue) outputListValue.get(0);
        assertThat(
                firstItem.getTypeVertex().get().getCanonicalType(), equalToIgnoringCase("Account"));
        assertThat(firstItem.isIndeterminant(), equalTo(true));
        assertThat(
                firstItem.getApexValueProperties().size(),
                equalTo(0)); // TODO: weird check, but compilation fails on Matchers.hasSize()
    }

    @Test
    public void testAccessDecisionValueFromSoql() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = [SELECT Id, Name FROM Account];\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		List<Account> sanitizedValues = sd.getRecords();\n"
                    + "		System.debug(sanitizedValues);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(
                outputListValue.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));
        assertThat(outputListValue.isIndeterminant(), equalTo(true));

        final Typeable typeable = outputListValue.getListType().get();
        assertThat(typeable.getCanonicalType(), equalToIgnoringCase("Account"));
    }

    @Test
    public void testAccessDecisionValueFromInlineSoql() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, [SELECT Id, Name FROM Account]);\n"
                    + "		List<Account> sanitizedValues = sd.getRecords();\n"
                    + "		System.debug(sanitizedValues);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(
                outputListValue.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));
        assertThat(outputListValue.isIndeterminant(), equalTo(true));

        final Typeable typeable = outputListValue.getListType().get();
        assertThat(typeable.getCanonicalType(), equalToIgnoringCase("Account"));
    }

    @Test
    public void testAccessDecisionValueInForLoopInline() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		for (Account a : sd.getRecords()) {\n"
                    + "			System.debug(a);\n"
                    + "		}\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();

        final List<ApexValue<?>> apexValues = forLoopValue.getForLoopValues();
        assertThat(apexValues, hasSize(1));
        final ApexSingleValue firstItem = (ApexSingleValue) apexValues.get(0);

        assertThat(
                firstItem.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));
        assertThat(
                firstItem.getTypeVertex().get().getCanonicalType(), equalToIgnoringCase("Account"));
    }

    @Test
    public void testAccessDecisionValueInForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		List<Account> cleaned = sd.getRecords();\n"
                    + "		for (Account a : cleaned) {\n"
                    + "			System.debug(a);\n"
                    + "		}\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();

        final List<ApexValue<?>> apexValues = forLoopValue.getForLoopValues();
        assertThat(apexValues, hasSize(1));
        final ApexSingleValue firstItem = (ApexSingleValue) apexValues.get(0);

        assertThat(
                firstItem.isSanitized(
                        MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                        CREATEABLE_ACCESS_TYPE),
                equalTo(true));
        assertThat(
                firstItem.getTypeVertex().get().getCanonicalType(), equalToIgnoringCase("Account"));
        assertThat(firstItem.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testIndeterminantAccessType() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(System.AccessType accessType) {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(accessType, acc);\n"
                    + "		System.debug(sd.getRecords());\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexListValue outputListValue = visitor.getSingletonResult();
        assertThat(outputListValue.isIndeterminant(), equalTo(false));
        assertThat(
                outputListValue.getListType().get().getCanonicalType(),
                equalToIgnoringCase("Account"));
    }

    @Test
    public void testGetModifiedIndexes() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		System.debug(sd.getModifiedIndexes());\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexSetValue outputSetValue = visitor.getSingletonResult();
        assertThat(outputSetValue.isIndeterminant(), equalTo(true));
        assertThat(outputSetValue.getDeclaredType().get(), equalToIgnoringCase("Set<Integer>"));
    }

    @Test
    public void testGetRemovedFields() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<Account> acc = new List<Account>();\n"
                    + "		acc.add(new Account(Name = 'Acme Inc'));\n"
                    + "		SObjectAccessDecision sd = Security.stripInaccessible(AccessType.CREATABLE, acc);\n"
                    + "		System.debug(sd.getRemovedFields());\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexMapValue outputMapValue = visitor.getSingletonResult();
        assertThat(outputMapValue.isIndeterminant(), equalTo(true));
        assertThat(outputMapValue.getKeyType(), equalToIgnoringCase("String"));
        assertThat(outputMapValue.getValueType(), equalToIgnoringCase("Set<String>"));
    }

    @Test
    public void testAccessDecisionValuePassedIn() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public static void doSomething() {\n"
                        + "		SObjectAccessDecision ad = Security.stripInaccessible(AccessType.READABLE, [SELECT Id, Name FROM Account]);\n"
                        + "		debug(ad);\n"
                        + "	}\n"
                        + "	public static void debug(SObjectAccessDecision ad) {\n"
                        + "		System.debug(ad);\n"
                        + "	}\n"
                        + "}\n";

        GraphTraversalSource g = TestUtil.getGraph();
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectAccessDecision value = visitor.getSingletonResult();
        assertThat(
                value.getAccessType(),
                optEqualTo(FlsConstants.StripInaccessibleAccessType.READABLE));
        assertThat(value.getSanitizableValue().isPresent(), equalTo(true));
        assertThat(value.getSanitizableValue().get() instanceof ApexSoqlValue, equalTo(true));
    }

    @Test
    public void testApexAccessDecisionValueAsMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(SObjectAccessDecision ad) {\n"
                        + "    	System.debug(ad);\n"
                        + "    }\n"
                        + "}\n";

        GraphTraversalSource g = TestUtil.getGraph();
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        SObjectAccessDecision value = visitor.getSingletonResult();

        assertThat(value.getAccessType().isPresent(), equalTo(false));
        assertThat(value.getSanitizableValue().isPresent(), equalTo(false));
    }
}

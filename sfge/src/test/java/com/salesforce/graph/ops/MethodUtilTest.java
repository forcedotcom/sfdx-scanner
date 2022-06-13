package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MethodUtilTest {
    private GraphTraversalSource g;

    private static final String METHOD_THAT_DOES_NOT_EXIST = "methodThatDoesNotExist";
    private static final String METHOD_WITHOUT_OVERLOADS_1 = "methodWithoutOverloads1";
    private static final String METHOD_WITHOUT_OVERLOADS_2 = "methodWithoutOverloads2";
    private static final String METHOD_WITHOUT_OVERLOADS_3 = "methodWithoutOverloads3";
    private static final String METHOD_WITH_INTERNAL_OVERLOADS = "methodWithInternalOverloads";
    private static final String METHOD_WITH_EXTERNAL_NAME_DUPLICATION =
            "methodWithExternalNameDuplication";
    private static final String METHOD_WITH_INNER_CLASS_DUPLICATION =
            "methodWithInnerClassDuplication";

    private static final String SOURCE_FILE_1 =
            "public class Foo1 {\n"
                    + "	public boolean "
                    + METHOD_WITHOUT_OVERLOADS_1
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITHOUT_OVERLOADS_2
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITH_INTERNAL_OVERLOADS
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITH_INTERNAL_OVERLOADS
                    + "(boolean b) {\n"
                    + "		return b;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITH_EXTERNAL_NAME_DUPLICATION
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITH_EXTERNAL_NAME_DUPLICATION
                    + "(boolean b) {\n"
                    + "		return b;\n"
                    + "	}\n"
                    + "}\n";

    private static final String SOURCE_FILE_2 =
            "public class Foo2 {\n"
                    + "	public boolean "
                    + METHOD_WITH_EXTERNAL_NAME_DUPLICATION
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "\n"
                    + "	public boolean "
                    + METHOD_WITH_EXTERNAL_NAME_DUPLICATION
                    + "(boolean b) {\n"
                    + "		return b;\n"
                    + "	}\n"
                    + "}\n";

    private static final String SOURCE_FILE_3 =
            "public class Foo3 {\n"
                    + "	public boolean "
                    + METHOD_WITH_INNER_CLASS_DUPLICATION
                    + "() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "	\n"
                    + "	public class InnerFoo {\n"
                    + "		public boolean "
                    + METHOD_WITHOUT_OVERLOADS_3
                    + "() {\n"
                    + "			return true;\n"
                    + "		}\n"
                    + "	\n"
                    + "		public boolean "
                    + METHOD_WITH_INNER_CLASS_DUPLICATION
                    + "() {\n"
                    + "			return true;\n"
                    + "		}\n"
                    + "	}\n"
                    + "}\n";

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
        CliMessager.getInstance().resetMessages();
    }

    @Test
    public void getTargetMethods_targetSingleMethod() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing only the first non-overloaded method in the first file.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0", Collections.singletonList(METHOD_WITHOUT_OVERLOADS_1)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(1)));
        MethodVertex firstVertex = methodVertices.get(0);
        assertEquals(METHOD_WITHOUT_OVERLOADS_1, firstVertex.getName());

        String messages = CliMessager.getInstance().getAllMessages();
        assertEquals("[]", messages);
    }

    @Test
    public void getTargetMethods_targetSingleMethodCaseInsensitive() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing only the first non-overloaded method in the first file.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0",
                        Collections.singletonList(METHOD_WITHOUT_OVERLOADS_1.toLowerCase())));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(1)));
        MethodVertex firstVertex = methodVertices.get(0);
        assertEquals(METHOD_WITHOUT_OVERLOADS_1, firstVertex.getName());

        String messages = CliMessager.getInstance().getAllMessages();
        assertEquals("[]", messages);
    }

    @Test
    public void getTargetMethods_targetMultipleMethods() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing both non-overloaded methods in the first file.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0",
                        Arrays.asList(METHOD_WITHOUT_OVERLOADS_1, METHOD_WITHOUT_OVERLOADS_2)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(2)));
        MethodVertex firstVertex = methodVertices.get(0);
        assertEquals(METHOD_WITHOUT_OVERLOADS_1, firstVertex.getName());

        MethodVertex secondVertex = methodVertices.get(1);
        assertEquals(METHOD_WITHOUT_OVERLOADS_2, secondVertex.getName());

        String messages = CliMessager.getInstance().getAllMessages();
        assertEquals("[]", messages);
    }

    @Test
    public void getTargetMethods_targetOverloadedMethods() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing only the overloaded method in the first file.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0", Collections.singletonList(METHOD_WITH_INTERNAL_OVERLOADS)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(2)));
        MethodVertex firstVertex = methodVertices.get(0);
        assertEquals(METHOD_WITH_INTERNAL_OVERLOADS, firstVertex.getName());

        MethodVertex secondVertex = methodVertices.get(1);
        assertEquals(METHOD_WITH_INTERNAL_OVERLOADS, secondVertex.getName());

        String messages = CliMessager.getInstance().getAllMessages();
        MatcherAssert.assertThat(
                messages,
                containsString(EventKey.WARNING_MULTIPLE_METHOD_TARGET_MATCHES.getMessageKey()));
    }

    @Test
    public void getTargetMethods_targetNameDupedMethods() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing only the method in the first file whose name is used
        // elsewhere.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0",
                        Collections.singletonList(METHOD_WITH_EXTERNAL_NAME_DUPLICATION)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(2)));
        MethodVertex firstVertex = methodVertices.get(0);
        assertEquals(METHOD_WITH_EXTERNAL_NAME_DUPLICATION, firstVertex.getName());
        assertEquals(18, firstVertex.getBeginLine());

        MethodVertex secondVertex = methodVertices.get(1);
        assertEquals(METHOD_WITH_EXTERNAL_NAME_DUPLICATION, secondVertex.getName());
        assertEquals(22, secondVertex.getBeginLine());

        String messages = CliMessager.getInstance().getAllMessages();
        MatcherAssert.assertThat(
                messages,
                containsString(EventKey.WARNING_MULTIPLE_METHOD_TARGET_MATCHES.getMessageKey()));
    }

    @Test
    public void getTargetMethods_targetMethodDoesNotExist() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_1, SOURCE_FILE_2}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing a method that doesn't actually exist with that name.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0", Collections.singletonList(METHOD_THAT_DOES_NOT_EXIST)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(0)));

        String messages = CliMessager.getInstance().getAllMessages();
        MatcherAssert.assertThat(
                messages,
                containsString(EventKey.WARNING_NO_METHOD_TARGET_MATCHES.getMessageKey()));
    }

    @Test
    public void getTargetMethods_targetMethodInInnerClass() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_3}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing the method that exists only in the inner class.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0", Collections.singletonList(METHOD_WITHOUT_OVERLOADS_3)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(1)));
    }

    @Test
    public void getTargetMethods_targetMethodInInnerAndOuterClass() {
        TestUtil.Config config =
                TestUtil.Config.Builder.get(g, new String[] {SOURCE_FILE_3}).build();
        TestUtil.buildGraph(config);
        // Create a rule target encompassing the method that exists only in the inner class.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(
                new RuleRunnerTarget(
                        "TestCode0",
                        Collections.singletonList(METHOD_WITH_INNER_CLASS_DUPLICATION)));

        List<MethodVertex> methodVertices = MethodUtil.getTargetedMethods(g, targets);

        MatcherAssert.assertThat(methodVertices, hasSize(equalTo(2)));
        boolean line2Found = false;
        boolean line11Found = false;
        for (MethodVertex methodVertex : methodVertices) {
            assertEquals(METHOD_WITH_INNER_CLASS_DUPLICATION, methodVertex.getName());
            if (methodVertex.getBeginLine() == 2) {
                line2Found = true;
            } else if (methodVertex.getBeginLine() == 11) {
                line11Found = true;
            } else {
                fail("Unexpected line number " + methodVertex.getBeginLine());
            }
        }
        assertTrue(line2Found);
        assertTrue(line11Found);
        String messages = CliMessager.getInstance().getAllMessages();
        MatcherAssert.assertThat(
                messages,
                containsString(EventKey.WARNING_MULTIPLE_METHOD_TARGET_MATCHES.getMessageKey()));
    }
}

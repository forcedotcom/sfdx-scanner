package com.salesforce.graph.build;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class StaticAndAnonymousCodeBlockTest extends MethodPathBuilderTest {
	@Test
	public void testStaticBlock() {
		String[] sourceCode =
			{
				"public class StaticBlockClass {\n" +
				"	static {\n"
				+ "		System.debug('static block');\n"
				+ "	}\n"
				+ " public static String foo() {\n" +
					"	return 'hello';\n" +
					"}\n"
				+ "}",
				"public class MyClass {\n" +
					"	public void doSomething() {\n" +
					"		StaticBlockClass sb = new StaticBlockClass();\n" +
					"		System.debug(sb.foo());\n" +
					"	}\n" +
					"}"
			};

//		TestUtil.buildGraph(g, sourceCode);

		TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g,
			sourceCode);
		SystemDebugAccumulator visitor = result.getVisitor();
		final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

		assertThat(allResults, hasSize(2));

		// verify that static block is invoked first
		ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
		assertThat(stringValue.getValue().get(), equalTo("static block"));

	}
}

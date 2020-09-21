@isTest
public class AnotherTestClass {
	private static String ILLNAMEDSTRING = 'abcde'; // PMD has a rule requiring non-final variables to not have all-caps names.

	@isTest
	testMethod static void testWithoutAssertions() { // PMD has a rule requiring tests to have assertions.
		String s1 = 'abcdefg';
	}
}

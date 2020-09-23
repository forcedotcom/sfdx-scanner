@isTest
public class SomeOtherTestClass {
	@isTest
	testMethod static void someTestMethodWithAsserts() {
		System.assert(4 * 3 - 2 == 10, 'Oops, turns out math is broken');
		System.assert((5 * 5) - 7 + 2 == 19, 'Oops, turns out math is broken');
		System.assert(!!true == true, 'Oops, turns out logic is broken');
	}

	@isTest
	testMethod static void someTestMethodWithoutAsserts() { // There's a PMD rule requiring tests to have an assert call.
		System.debug(LoggingLevel.DEBUG, 'There are no asserts in this method');
	}

	@isTest
	testMethod static void anotherTestMethodWithAsserts() {
		DateTime d1 = DateTime.now();
		DateTime d2 = DateTime.now();
		System.assert(d1 != d2, 'Assertion failed');
	}
}

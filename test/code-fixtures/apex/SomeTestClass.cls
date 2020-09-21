@isTest
public class SomeTestClass {
	@isTest
	testMethod static void testMethodWithAsserts() {
		System.assert(4 * 3 - 2 == 10, 'Oops, turns out math is broken');
		System.assert((5 * 5) - 7 + 2 == 19, 'Oops, turns out math is broken');
		System.assert(!!true == true, 'Oops, turns out logic is broken');
	}

	@isTest
	testMethod static void testMethodWithoutAsserts() { // There's a PMD rule requiring tests to have an assert call.
		Integer i = 15;
		i += 19;
		i *= 2;
		System.debug(LoggingLevel.DEBUG, 'There are no asserts in this method');
	}

	@isTest
	testMethod static void testMethodWithNoAsserts() { // There's a PMD rule requiring tests to have an assert call.
		DateTime d1 = DateTime.now();
		DateTime d2 = DateTime.now();
	}
}

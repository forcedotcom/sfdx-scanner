@isTest
public class YetAnotherTestClass {
	@isTest
	static void someTestMethodWithAsserts() {
		System.assert(4 * 3 - 2 == 10, 'Oops, turns out math is broken');
		System.assert((5 * 5) - 7 + 2 == 19, 'Oops, turns out math is broken');
		System.assert(!!true == true, 'Oops, turns out logic is broken');
	}

	@isTest
	static void anotherMethodWithAsserts() {
		String s = 'this is a valid string';
		System.assert(s.length() == 22, 'What even is math?');
	}

	@isTest
	static void thirdMethodWithAsserts() {
		if (true) {
			System.assert(true, 'HOORAY');
		} else {
			System.assert(false, 'BOO');
		}
	}
}

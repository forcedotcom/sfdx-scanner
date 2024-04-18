import {expect} from "chai";
import {verifyMain2DevPrTitle} from "../src/verifyMain2DevPrTitle";


describe('#verifyMain2DevPrTitle', () => {
	/**
	 * The Type portion is the first part of the title.
	 * E.g., in "Main2Dev @W-1234@ - v4.2.0", the Type portion is "Main2Dev".
	 * Only acceptable value is Main2Dev, with flexible casing.
	 */
	describe('Type portion', () => {
		const restOfTitle = "@W-111111@ Rebasing after v4.2.0";
		describe('Valid types', () => {
			it('Uppercase: MAIN2DEV', () => {
				const title = `MAIN2DEV ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('Lowercase: main2dev', () => {
				const title = `main2dev ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('Spongecase: mAiN2DeV', () => {
				const title = `mAiN2DeV ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});
		});

		describe('Invalid types', () => {
			describe('NEW', () => {
				it('Uppercase: NEW', () => {
					const title = `NEW ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Lowercase: new', () => {
					const title = `new ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Spongecase: NeW', () => {
					const title = `NeW ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});
			});

			describe('FIX', () => {
				it('Uppercase: FIX', () => {
					const title = `FIX ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Lowercase: fix', () => {
					const title = `fix ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Spongecase: FiX', () => {
					const title = `FiX ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});
			});

			describe('CHANGE', () => {
				it('Uppercase: CHANGE', () => {
					const title = `CHANGE ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Lowercase: change', () => {
					const title = `change ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Spongecase: ChAnGe', () => {
					const title = `ChAnGe ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});
			});

			describe('BEEP (i.e., something random)', () => {
				it('Uppercase: BEEP', () => {
					const title = `BEEP ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Lowercase: beep', () => {
					const title = `beep ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});

				it('Spongecase: BeEp', () => {
					const title = `BeEp ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
				});
			});
		});
	});

	/**
	 * The Scope portion is not used for Release pull requests.
	 */
	it('Scope portion is rejected', () => {
		const title = "Main2Dev (PMD) @W-1234@ Rebasing after v4.2.0";
		expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
	});

	/**
	 * The Work Item portion is the third part of the title.
	 * E.g., in "Main2Dev @W-1234@ - Rebasing after v4.2.0", the Work Item portion is "@W-1234@".
	 */
	describe('Work Item portion', () => {
		function createTitle(workItem: string): string {
			return `Main2Dev ${workItem} Rebasing after v4.2.0`;
		}

		it('Work Item cannot be absent', () => {
			const title = createTitle('');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Work Item requires leading @-symbol', () => {
			const title = createTitle('W-1234@');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Work Item requires trailing @-symbol', () => {
			const title = createTitle('@W-1234');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Work Item must start with "W-"', () => {
			const title = createTitle('@1234@');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		})

		it('Work Item cannot be non-numeric', () => {
			const title = createTitle('@W-XXXX@');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Work Item cannot be below minimum', () => {
			const title = createTitle('@W-1@');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Work Item cannot be above maximum', () => {
			const title = createTitle('@W-1000000000000@');
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Valid Work Item is accepted', () => {
			const title = createTitle('@W-12345678@');
			expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
		});
	});

	/**
	 * The Description portion is the fourth (and last) part of the title.
	 * E.g., in "Main2Dev @W-1234@ - Rebasing after v4.2.0", the Description portion is "Rebasing after v4.2.0"
	 */
	describe('Description portion', () => {
		const restOfTitle = "Main2Dev @W-123456@";

		it('Description cannot be absent', () => {
			const title = `${restOfTitle}`;
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Description cannot be empty', () => {
			const title = `${restOfTitle} `;
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Description must contain "rebasing"', () => {
			const title = `${restOfTitle} after v4.2.0`;
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		});

		it('Description must contain "vX.Y.Z"', () => {
			const title = `${restOfTitle} rebasing`;
			expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
		})

		it('Description can contain any character classes', () => {
			const title = `${restOfTitle} rebasing asdfasdf334 v4.2.0 @@$%#!{}`;
			expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
		});
	});

	/**
	 * The portions should be separated from each other by either whitespace or a flexible group
	 * of special characters.
	 * E.g., "Main2Dev ; @W-1234@ . Rebasing after v4.2.0".
	 */
	describe('Portion separation', () => {
		it('Space (" ") separation is allowed', () => {
			const title = "Main2Dev     @W-1234@  rebasing after  v4.2.0";
			expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
		});

		describe('Separator characters are allowed', () => {
			it('n-dash (-)', () => {
				const title = "Main2Dev - @W-1234@ - rebasing after v4.2.0";
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('period (.)', () => {
				const title = "Main2Dev.@W-1234@.rebasing after v4.2.0";
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('comma (,)', () => {
				const title = "Main2Dev,@W-1234@,rebasing after v4.2.0";
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('colon (:)', () => {
				const title = "Main2Dev:@W-1234@:rebasing after v4.2.0";
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});

			it('semi colon (;)', () => {
				const title = "Main2Dev;@W-1234@;rebasing after v4.2.0";
				expect(verifyMain2DevPrTitle(title)).to.equal(true, 'Should be accepted');
			});
		});

		describe('Unrecognized separators are not allowed', () => {
			it('Pipe (|)', () => {
				const title = 'Main2Dev|@W-1234@|Added whatever';
				expect(verifyMain2DevPrTitle(title)).to.equal(false, 'Should be rejected');
			})
		});
	});
});

import {verifyReleasePrTitle} from "../src/verifyReleasePrTitle";

describe('#verifyReleasePrTitle', () => {
	/**
	 * The Type portion is the first part of the title.
	 * E.g., in "RELEASE @W-1234@ - v5.2.0", the Type portion is "RELEASE".
	 * Only acceptable value is RELEASE, with flexible casing.
	 */
	describe('Type portion', () => {
		const restOfTitle = "@W-111111@ Filler text";
		describe('Valid types', () => {
			it('Uppercase: RELEASE', () => {
				const title = `RELEASE ${restOfTitle}`
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('Lowercase: release', () => {
				const title = `release ${restOfTitle}`
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('Spongecase: rElEaSe', () => {
				const title = `rElEaSe ${restOfTitle}`
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});
		});

		describe('Invalid types', () => {
			describe('NEW', () => {
				it('Uppercase: NEW', () => {
					const title = `NEW ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Lowercase: new', () => {
					const title = `new ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Spongecase: NeW', () => {
					const title = `NeW ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});
			});

			describe('FIX', () => {
				it('Uppercase: FIX', () => {
					const title = `FIX ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Lowercase: fix', () => {
					const title = `fix ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Spongecase: FiX', () => {
					const title = `FiX ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});
			});

			describe('CHANGE', () => {
				it('Uppercase: CHANGE', () => {
					const title = `CHANGE ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Lowercase: change', () => {
					const title = `change ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Spongecase: ChAnGe', () => {
					const title = `ChAnGe ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});
			});

			describe('BEEP (i.e., something random)', () => {
				it('Uppercase: BEEP', () => {
					const title = `BEEP ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Lowercase: beep', () => {
					const title = `beep ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});

				it('Spongecase: BeEp', () => {
					const title = `BeEp ${restOfTitle}`
					expect(verifyReleasePrTitle(title)).toEqual(false);
				});
			});
		});
	});

	/**
	 * The Scope portion is not used for Release pull requests.
	 */
	it('Scope portion is rejected', () => {
		const title = "RELEASE (PMD) @W-1234@ v5.2.0";
		expect(verifyReleasePrTitle(title)).toEqual(false);
	});

	/**
	 * The Work Item portion is the third part of the title.
	 * E.g., in "RELEASE @W-1234@ - v5.2.0", the Work Item portion is "@W-1234@".
	 */
	describe('Work Item portion', () => {
		function createTitle(workItem: string): string {
			return `RELEASE ${workItem} Filler text`;
		}

		it('Work Item cannot be absent', () => {
			const title = createTitle('');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Work Item requires leading @-symbol', () => {
			const title = createTitle('W-1234@');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Work Item requires trailing @-symbol', () => {
			const title = createTitle('@W-1234');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Work Item must start with "W-"', () => {
			const title = createTitle('@1234@');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		})

		it('Work Item cannot be non-numeric', () => {
			const title = createTitle('@W-XXXX@');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be below minimum', () => {
			const title = createTitle('@W-1@');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be above maximum', () => {
			const title = createTitle('@W-1000000000000@');
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Valid Work Item is accepted', () => {
			const title = createTitle('@W-12345678@');
			expect(verifyReleasePrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The Description portion is the fourth (and last) part of the title.
	 * E.g., in "RELEASE @W-1234@ - v5.2.0", the Description portion is "v5.2.0"
	 */
	describe('Description portion', () => {
		const restOfTitle = "RELEASE @W-123456@";

		it('Description cannot be absent', () => {
			const title = `${restOfTitle}`;
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Description cannot be empty', () => {
			const title = `${restOfTitle} `;
			expect(verifyReleasePrTitle(title)).toEqual(false);
		});

		it('Description can contain any character classes', () => {
			const title = `${restOfTitle} asdfasdf334@@$%#!{}.`;
			expect(verifyReleasePrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The portions should be separated from each other by either whitespace or a flexible group
	 * of special characters.
	 * E.g., "NEW - (PMD) ; @W-1234@ . Added whatever".
	 */
	describe('Portion separation', () => {
		it('Space (" ") separation is allowed', () => {
			const title = "RELEASE  @W-1234@        v5.2.0";
			expect(verifyReleasePrTitle(title)).toEqual(true);
		});

		describe('Separator characters are allowed', () => {
			it('n-dash (-)', () => {
				const title = "RELEASE - @W-1234@ - v5.2.0";
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('period (.)', () => {
				const title = "RELEASE.@W-1234@.v5.2.0";
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('comma (,)', () => {
				const title = "RELEASE,@W-1234@,v5.2.0";
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('colon (:)', () => {
				const title = "RELEASE:@W-1234@:v5.2.0";
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});

			it('semi colon (;)', () => {
				const title = "RELEASE;@W-1234@;v5.2.0";
				expect(verifyReleasePrTitle(title)).toEqual(true);
			});
		});

		describe('Unrecognized separators are not allowed', () => {
			it('Pipe (|)', () => {
				const title = 'RELEASE|@W-1234@|Added whatever';
				expect(verifyReleasePrTitle(title)).toEqual(false);
			})
		});
	});
});

import {verifyFeaturePrTitle} from "../src/verifyFeaturePrTitle";

describe('#verifyFeaturePrTitle', () => {
	/**
	 * The Type portion is the first part of the title.
	 * E.g., in "NEW (PMD) @W-1234@ - Added whatever", the Type portion is "NEW".
	 * Acceptable values are NEW, FIX, and CHANGE, with flexible casing.
	 */
	describe('Type portion', () => {
		const restOfTitle = '(filler) @W-111111@ Filler text';

		describe('Valid types', () => {
			describe('NEW', () => {
				it('Uppercase: NEW', () => {
					const title = `NEW ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Lowercase: new', () => {
					const title = `new ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Spongecase: nEw', () => {
					const title = `nEw ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});
			});

			describe('FIX', () => {
				it('Uppercase: FIX', () => {
					const title = `FIX ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Lowercase: fix', () => {
					const title = `fix ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Spongecase: fIx', () => {
					const title = `fIx ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});
			});

			describe('CHANGE', () => {
				it('Uppercase: CHANGE', () => {
					const title = `CHANGE ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Lowercase: change', () => {
					const title = `change ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});

				it('Spongecase: cHaNgE', () => {
					const title = `cHaNgE ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(true);
				});
			});
		});

		describe('Invalid types', () => {
			describe('RELEASE', () => {
				it('Uppercase: RELEASE', () => {
					const title = `RELEASE ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});

				it('Lowercase: release', () => {
					const title = `release ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});

				it('Spongecase: rElEaSe', () => {
					const title = `rElEaSe ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});
			});

			describe('BEEP (i.e., something random)', () => {
				it('Uppercase: BEEP', () => {
					const title = `BEEP ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});

				it('Lowercase: beep', () => {
					const title = `beep ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});

				it('Spongecase: bEeP', () => {
					const title = `bEeP ${restOfTitle}`;
					expect(verifyFeaturePrTitle(title)).toEqual(false);
				});
			});
		});
	});

	/**
	 * The Scope portion is the second part of the title.
	 * E.g., in "NEW (PMD) @W-1234@ - Added whatever", the Scope portion is "(PMD)".
	 * The Scope can be anything the author wants, and is technically optional, but our general hope is that it would
	 * correspond to a product domain (e.g., PMD, ESLint, etc).
	 */
	describe('Scope portion', () => {
		function createTitle(scope: string): string {
			return `NEW ${scope} @W-1234@ Filler text`;
		}

		it('Scope portion may absent', () => {
			const title = createTitle('');
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});

		it('Scope portion cannot be empty if present', () => {
			const title = createTitle('()');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Scope portion must be properly wrapped in parentheses', () => {
			const title = createTitle('asdfasdf');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		})

		it('Scope cannot contain parentheses', () => {
			const title = createTitle('(asd(eaae))');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Scope can be alphanumeric', () => {
			const title = createTitle('(abcd1234)');
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});

		it('Scope can contain non-parenthesis special characters', () => {
			const title = createTitle('( ,.\|{}[]_L!@#$%^)');
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The Work Item portion is the third part of the title.
	 * E.g., in "NEW (PMD) @W-1234@ - Added whatever", the Work Item portion is "@W-1234@".
	 */
	describe('Work Item portion', () => {
		function createTitle(workItem: string): string {
			return `NEW (PMD) ${workItem} Filler text`;
		}

		it('Work Item cannot be absent', () => {
			const title = createTitle('');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Work Item requires leading @-symbol', () => {
			const title = createTitle('W-1234@');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Work Item requires trailing @-symbol', () => {
			const title = createTitle('@W-1234');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Work Item must start with "W-"', () => {
			const title = createTitle('@1234@');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		})

		it('Work Item cannot be non-numeric', () => {
			const title = createTitle('@W-XXXX@');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be below minimum', () => {
			const title = createTitle('@W-1@');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be above maximum', () => {
			const title = createTitle('@W-1000000000000@');
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Valid Work Item is accepted', () => {
			const title = createTitle('@W-12345678@');
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The Description portion is the fourth (and last) part of the title.
	 * E.g., in "NEW (PMD) @W-1234@ - Added whatever", the Description portion is "Added whatever"
	 */
	describe('Description portion', () => {
		const restOfTitle = "NEW (PMD) @W-123456@";

		it('Description cannot be absent', () => {
			const title = `${restOfTitle}`;
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Description cannot be empty', () => {
			const title = `${restOfTitle} `;
			expect(verifyFeaturePrTitle(title)).toEqual(false);
		});

		it('Description can contain any character classes', () => {
			const title = `${restOfTitle} asdfasdf334@@$%#!{}.`;
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The portions should be separated from each other by either whitespace or a flexible group
	 * of special characters.
	 * E.g., "NEW - (PMD) ; @W-1234@ . Added whatever".
	 */
	describe('Portion separation', () => {
		it('Space (" ") separation is allowed', () => {
			const title = "NEW     (PMD)  @W-1234@          Added whatever";
			expect(verifyFeaturePrTitle(title)).toEqual(true);
		});

		describe('Separator characters are allowed', () => {
			it('n-dash (-)', () => {
				const title = "NEW - (PMD) - @W-1234@ - Added whatever";
				expect(verifyFeaturePrTitle(title)).toEqual(true);
			});

			it('period (.)', () => {
				const title = "NEW.(PMD).@W-1234@.Added whatever";
				expect(verifyFeaturePrTitle(title)).toEqual(true);
			});

			it('comma (,)', () => {
				const title = "NEW,(PMD),@W-1234@,Added whatever";
				expect(verifyFeaturePrTitle(title)).toEqual(true);
			});

			it('colon (:)', () => {
				const title = "NEW:(PMD):@W-1234@:Added whatever";
				expect(verifyFeaturePrTitle(title)).toEqual(true);
			});

			it('semi colon (;)', () => {
				const title = "NEW;(PMD);@W-1234@;Added whatever";
				expect(verifyFeaturePrTitle(title)).toEqual(true);
			});
		});

		describe('Unrecognized separators are not allowed', () => {
			it('Pipe (|)', () => {
				const title = 'NEW | (PMD) | @W-1234@ | Added whatever';
				expect(verifyFeaturePrTitle(title)).toEqual(false);
			})
		});
	});
});

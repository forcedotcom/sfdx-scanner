import {verifyMain2DevPrTitle} from "../src/verifyMain2DevPrTitle";


describe('#verifyMain2DevPrTitle', () => {
	/**
	 * The Type portion is the first part of the title.
	 * E.g., in "Main2Dev @W-1234@ - v5.2.0", the Type portion is "Main2Dev".
	 * Only acceptable value is Main2Dev, with flexible casing.
	 */
	describe('Type portion', () => {
		const restOfTitle = "@W-111111@ Merging after v5.2.0";
		describe('Valid types', () => {
			it('Uppercase: MAIN2DEV', () => {
				const title = `MAIN2DEV ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('Lowercase: main2dev', () => {
				const title = `main2dev ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('Spongecase: mAiN2DeV', () => {
				const title = `mAiN2DeV ${restOfTitle}`
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});
		});

		describe('Invalid types', () => {
			describe('NEW', () => {
				it('Uppercase: NEW', () => {
					const title = `NEW ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Lowercase: new', () => {
					const title = `new ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Spongecase: NeW', () => {
					const title = `NeW ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});
			});

			describe('FIX', () => {
				it('Uppercase: FIX', () => {
					const title = `FIX ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Lowercase: fix', () => {
					const title = `fix ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Spongecase: FiX', () => {
					const title = `FiX ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});
			});

			describe('CHANGE', () => {
				it('Uppercase: CHANGE', () => {
					const title = `CHANGE ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Lowercase: change', () => {
					const title = `change ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Spongecase: ChAnGe', () => {
					const title = `ChAnGe ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});
			});

			describe('BEEP (i.e., something random)', () => {
				it('Uppercase: BEEP', () => {
					const title = `BEEP ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Lowercase: beep', () => {
					const title = `beep ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});

				it('Spongecase: BeEp', () => {
					const title = `BeEp ${restOfTitle}`
					expect(verifyMain2DevPrTitle(title)).toEqual(false);
				});
			});
		});
	});

	/**
	 * The Scope portion is not used for Release pull requests.
	 */
	it('Scope portion is rejected', () => {
		const title = "Main2Dev (PMD) @W-1234@ Merging after v5.2.0";
		expect(verifyMain2DevPrTitle(title)).toEqual(false);
	});

	/**
	 * The Work Item portion is the third part of the title.
	 * E.g., in "Main2Dev @W-1234@ - Merging after v5.2.0", the Work Item portion is "@W-1234@".
	 */
	describe('Work Item portion', () => {
		function createTitle(workItem: string): string {
			return `Main2Dev ${workItem} Merging after v5.2.0`;
		}

		it('Work Item cannot be absent', () => {
			const title = createTitle('');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Work Item requires leading @-symbol', () => {
			const title = createTitle('W-1234@');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Work Item requires trailing @-symbol', () => {
			const title = createTitle('@W-1234');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Work Item must start with "W-"', () => {
			const title = createTitle('@1234@');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		})

		it('Work Item cannot be non-numeric', () => {
			const title = createTitle('@W-XXXX@');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be below minimum', () => {
			const title = createTitle('@W-1@');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Work Item cannot be above maximum', () => {
			const title = createTitle('@W-1000000000000@');
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Valid Work Item is accepted', () => {
			const title = createTitle('@W-12345678@');
			expect(verifyMain2DevPrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The Description portion is the fourth (and last) part of the title.
	 * E.g., in "Main2Dev @W-1234@ - Merging after v5.2.0", the Description portion is "Merging after v5.2.0"
	 */
	describe('Description portion', () => {
		const restOfTitle = "Main2Dev @W-123456@";

		it('Description cannot be absent', () => {
			const title = `${restOfTitle}`;
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Description cannot be empty', () => {
			const title = `${restOfTitle} `;
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Description must contain "merging"', () => {
			const title = `${restOfTitle} after v5.2.0`;
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		});

		it('Description must contain "vX.Y.Z"', () => {
			const title = `${restOfTitle} merging`;
			expect(verifyMain2DevPrTitle(title)).toEqual(false);
		})

		it('Description can contain any character classes', () => {
			const title = `${restOfTitle} merging asdfasdf334 v5.2.0 @@$%#!{}`;
			expect(verifyMain2DevPrTitle(title)).toEqual(true);
		});
	});

	/**
	 * The portions should be separated from each other by either whitespace or a flexible group
	 * of special characters.
	 * E.g., "Main2Dev ; @W-1234@ . Merging after v5.2.0".
	 */
	describe('Portion separation', () => {
		it('Space (" ") separation is allowed', () => {
			const title = "Main2Dev     @W-1234@  merging after  v5.2.0";
			expect(verifyMain2DevPrTitle(title)).toEqual(true);
		});

		describe('Separator characters are allowed', () => {
			it('n-dash (-)', () => {
				const title = "Main2Dev - @W-1234@ - merging after v5.2.0";
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('period (.)', () => {
				const title = "Main2Dev.@W-1234@.merging after v5.2.0";
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('comma (,)', () => {
				const title = "Main2Dev,@W-1234@,merging after v5.2.0";
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('colon (:)', () => {
				const title = "Main2Dev:@W-1234@:merging after v5.2.0";
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});

			it('semi colon (;)', () => {
				const title = "Main2Dev;@W-1234@;merging after v5.2.0";
				expect(verifyMain2DevPrTitle(title)).toEqual(true);
			});
		});

		describe('Unrecognized separators are not allowed', () => {
			it('Pipe (|)', () => {
				const title = 'Main2Dev|@W-1234@|Added whatever';
				expect(verifyMain2DevPrTitle(title)).toEqual(false);
			})
		});
	});
});

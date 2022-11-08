import { expect } from "chai";
import { verifyDevBranchPrTitle, verifyReleaseBranchPrTitle } from "../src/verifyPrTitle";

describe("#verifyDevBranchPrTitle()", () => {
	describe("Positive Tests", () => {
		describe("Allow valid type values", () => {
			it("Type value: NEW", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): @W-1111@: This is a message.")).to.equal(true);
			});

			it("Type value: CHANGE", () => {
				expect(verifyDevBranchPrTitle("CHANGE (PMD): @W-1111@: beep boop bop.")).to.equal(true);
			});

			it("Type value: FIX", () => {
				expect(verifyDevBranchPrTitle("FIX (PMD): @W-1111@: beep boop bop.")).to.equal(true);
			});
		});

		describe("Allow multiple scopes", () => {
			it("Scopes: PMD + ESLint", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD|ESLint): @W-1111@: beep boop bop.")).to.equal(true);
			});

			it("Scopes: PMD + ESLint + RetireJS", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD|ESLint|RetireJS): @W-1111@: beep boop bop.")).to.equal(true);
			});
		});

		describe("Be flexible with input", () => {
			it("Be case-insensitive", () => {
				expect(verifyDevBranchPrTitle("nEw (PmD): @W-1111@: beep boop bop.")).to.equal(true);
			});

			it("Allow excessive spacing", () => {
				expect(verifyDevBranchPrTitle("   NEW   (PMD)  :    @W-1111@  :    beep boop bop.")).to.equal(true);
			});

			it("Allow no spacing", () => {
				expect(verifyDevBranchPrTitle("NEW(PMD):@W-1111@:beep boop bop.")).to.equal(true);
			});
		})
	});

	describe("Negative Tests", () => {
		it("Improperly-ordered PR title", () => {
			expect(verifyDevBranchPrTitle("(PMD) NEW: @W-1111@: beep boop bop.")).to.equal(false);
		});

		describe("Problems with PR type", () => {
			it("Invalid PR type", () => {
				expect(verifyDevBranchPrTitle("MEW (PMD): @W-1111@: beep boop bop.")).to.equal(false);
			});

			it("Multiple PR types", () => {
				expect(verifyDevBranchPrTitle("NEW CHANGE (PMD): @W-1111@: beep boop bop.")).to.equal(false);
			});

			it("Missing PR type", () => {
				expect(verifyDevBranchPrTitle("(PMD): @W-1111@: beep boop bop.")).to.equal(false);
			});
		});

		describe("Problems with PR scope", () => {
			it("Invalid PR scope", () => {
				expect(verifyDevBranchPrTitle("NEW (PMQ): @W-1111@: beep boop bop.")).to.equal(false);
			});

			it("Missing PR scope", () => {
				expect(verifyDevBranchPrTitle("NEW : @W-1111@: beep boop bop.")).to.equal(false);
			});

			it("Empty PR scope", () => {
				expect(verifyDevBranchPrTitle("NEW (): @W-1111@: beep boop bop.")).to.equal(false);
			});

			it("Improperly-delimited PR scopes", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD,CPD): @W-1111@: beep boop bop.")).to.equal(false);
			});
		});

		describe("Problems with PR work number", () => {
			it("Work item below minimum range", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): @W-999@: beep boop bop.")).to.equal(false);
			});

			it("Work item above maximum range", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): @W-100000000@: beep boop bop.")).to.equal(false);
			});

			it("Work item contains letters", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): @W-1003a00@: beep boop bop.")).to.equal(false);
			});

			it("Work item missing leading @-symbol", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): W-1000@: beep boop bop.")).to.equal(false);
			});

			it("Work item missing trailing @-symbol", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): @W-1000: beep boop bop.")).to.equal(false);
			});

			it("Work item missing", () => {
				expect(verifyDevBranchPrTitle("NEW (PMD): beep boop bop.")).to.equal(false);
			});
		});
	});
});
describe("#verifyReleaseBranchPrTitle()", () => {
	describe("Positive tests", () => {
		it("Happy path", () => {
			expect(verifyReleaseBranchPrTitle("RELEASE: @W-1111@: Releasing v3.6.2.")).to.equal(true);
		});

		describe("Be flexible with input", () => {
			it("Be case-insensitive", () => {
				expect(verifyReleaseBranchPrTitle("ReLeAsE: @w-1111@: Releasing v3.6.2")).to.equal(true);
			});

			it("Allow excessive spacing", () => {
				expect(verifyReleaseBranchPrTitle("     RELEASE    :    @W-1111@    :    Releasing v3.6.2")).to.equal(true);
			});

			it("Allow no spacing", () => {
				expect(verifyReleaseBranchPrTitle("RELEASE:@W-1111@:Releasingv3.6.2")).to.equal(true);
			});
		});
	});
	describe("Negative tests", () => {
		it("Improperly-ordered PR title", () => {
			expect(verifyReleaseBranchPrTitle("@W-1111@: RELEASE: Releasing v3.6.2")).to.equal(false);
		});

		it("Scope wrongfully included", () => {
			expect(verifyReleaseBranchPrTitle("RELEASE (PMD): @W-1111@: Releasing v3.6.2")).to.equal(false);
		});

		it("Disallowed type portion", () => {
			expect(verifyReleaseBranchPrTitle("REEEELEASE: @W-1111@: Releasing v3.6.2")).to.equal(false);
		});

		describe("Problems with PR work number", () => {
			it("Work item below minimum range", () => {
				expect(verifyDevBranchPrTitle("RELEASE: @W-999@: Releasing v3.6.2.")).to.equal(false);
			});

			it("Work item above maximum range", () => {
				expect(verifyDevBranchPrTitle("RELEASE: @W-1000000000@: Releasing v3.6.2.")).to.equal(false);
			});

			it("Work item contains letters", () => {
				expect(verifyDevBranchPrTitle("RELEASE: @W-1003a00@: Releasing v3.6.2.")).to.equal(false);
			});

			it("Work item missing leading @-symbol", () => {
				expect(verifyDevBranchPrTitle("RELEASE: W-1000@: Releasing v3.6.2.")).to.equal(false);
			});

			it("Work item missing trailing @-symbol", () => {
				expect(verifyDevBranchPrTitle("RELEASE: @W-1000: Releasing v3.6.2.")).to.equal(false);
			});

			it("Work item missing", () => {
				expect(verifyDevBranchPrTitle("RELEASE: beep boop bop.")).to.equal(false);
			});
		});
	});
});

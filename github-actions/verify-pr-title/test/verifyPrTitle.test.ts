import { expect } from "chai";
import { verifyPrTitleMatchesTemplate } from "../src/verifyPrTitle";

describe("Positive Tests", () => {
	it("Type: NEW; Scope: PMD", () => {
		expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-1111@: beep boop bop.")).to.equal(true);
	});

	it("Type: CHANGE; Scope: PMD", () => {
		expect(verifyPrTitleMatchesTemplate("CHANGE (PMD): @W-1111@: beep boop bop.")).to.equal(true);
	});

	it("Type: FIX; Scope: PMD", () => {
		expect(verifyPrTitleMatchesTemplate("FIX (PMD): @W-1111@: beep boop bop.")).to.equal(true);
	});

	it("Type: NEW; Scope: PMD + ESLint", () => {
		expect(verifyPrTitleMatchesTemplate("NEW (PMD|ESLint): @W-1111@: beep boop bop.")).to.equal(true);
	});

	it("Type: NEW; Scope: PMD + ESLint + RetireJS", () => {
		expect(verifyPrTitleMatchesTemplate("NEW (PMD|ESLint|RetireJS): @W-1111@: beep boop bop.")).to.equal(true);
	});

	it("Type: NEW; Scope: PMD", () => {
		expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-1111@: beep boop bop.")).to.equal(true);
	});
});

describe("Negative Tests", () => {
	it("Improperly-ordered PR title", () => {
		expect(verifyPrTitleMatchesTemplate("(PMD) NEW: @W-1111@: beep boop bop.")).to.equal(false);
	});

	describe("Problems with PR type", () => {
		it("Improperly-cased PR type", () => {
			expect(verifyPrTitleMatchesTemplate("new (PMD): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Invalid PR type", () => {
			expect(verifyPrTitleMatchesTemplate("MEW (PMD): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Multiple PR types", () => {
			expect(verifyPrTitleMatchesTemplate("NEW CHANGE (PMD): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Missing PR type", () => {
			expect(verifyPrTitleMatchesTemplate("(PMD): @W-1111@: beep boop bop.")).to.equal(false);
		});
	});

	describe("Problems with PR scope", () => {
		it("Improperly-cased PR scope", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (pmd): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Invalid PR scope", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMQ): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Missing PR scope", () => {
			expect(verifyPrTitleMatchesTemplate("NEW : @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Empty PR scope", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (): @W-1111@: beep boop bop.")).to.equal(false);
		});

		it("Improperly-delimited PR scopes", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD,CPD): @W-1111@: beep boop bop.")).to.equal(false);
		});
	});

	describe("Problems with PR work number", () => {
		it("Work item below minimum range", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-999@: beep boop bop.")).to.equal(false);
		});

		it("Work item above maximum range", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-100000000@: beep boop bop.")).to.equal(false);
		});

		it("Work item contains letters", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-1003a00@: beep boop bop.")).to.equal(false);
		});

		it("Work item missing leading @-symbol", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): W-1000@: beep boop bop.")).to.equal(false);
		});

		it("Work item missing trailing @-symbol", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): @W-1000: beep boop bop.")).to.equal(false);
		});

		it("Work item missing", () => {
			expect(verifyPrTitleMatchesTemplate("NEW (PMD): beep boop bop.")).to.equal(false);
		});
	});
});

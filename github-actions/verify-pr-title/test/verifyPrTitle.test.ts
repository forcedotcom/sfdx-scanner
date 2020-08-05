import { expect } from "chai";
import { verifyPRTitle } from "../src/verifyPrTitle";

describe("Positive Tests", () => {
	it("work item only", () => {
		expect(verifyPRTitle("@W-1234@")).to.equal(true);
	});

	it("work item at start of title", () => {
		expect(verifyPRTitle("@W-1234@ fix")).to.equal(true);
	});

	it("work item at end of title", () => {
		expect(verifyPRTitle("Fixes @W-1234@")).to.equal(true);
	});

	it("work item in middle of title", () => {
		expect(verifyPRTitle("Fixes @W-1234@ - something")).to.equal(true);
	});

	it("work item minium range", () => {
		expect(verifyPRTitle("@W-0000@")).to.equal(true);
	});

	it("work item maximum range", () => {
		expect(verifyPRTitle("@W-99999999@")).to.equal(true);
	});
});

describe("Negative Tests", () => {
		it("work item out of minimum range", () => {
		expect(verifyPRTitle("@W-999@")).to.equal(false);
	});

	it("work item out of maximum range", () => {
		expect(verifyPRTitle("@W-000000000@")).to.equal(false);
	});

	it("work item missing leading @ sign", () => {
		expect(verifyPRTitle("W-1234@")).to.equal(false);
	});

	it("work item missing trailing @ sign", () => {
		expect(verifyPRTitle("@W-1234")).to.equal(false);
	});

	it("work item invalid format", () => {
		expect(verifyPRTitle("@W-1234ab@")).to.equal(false);
	});
});

import { expect } from "chai";
import { verifyPRTitleForBugId } from "../src/verifyPrTitle";
import { verifyPRTitleForBadTitle } from "../src/verifyPrTitle";
import { verifyPRTitleForBaseBranch } from "../src/verifyPrTitle";

describe("Positive Tests", () => {
	it("work item only", () => {
		expect(verifyPRTitleForBugId("@W-1234@")).to.equal(true);
	});

	it("work item at start of title", () => {
		expect(verifyPRTitleForBugId("@W-1234@ fix")).to.equal(true);
	});

	it("work item at end of title", () => {
		expect(verifyPRTitleForBugId("Fixes @W-1234@")).to.equal(true);
	});

	it("work item in middle of title", () => {
		expect(verifyPRTitleForBugId("Fixes @W-1234@ - something")).to.equal(true);
	});

	it("work item minium range", () => {
		expect(verifyPRTitleForBugId("@W-0000@")).to.equal(true);
	});

	it("work item maximum range", () => {
		expect(verifyPRTitleForBugId("@W-99999999@")).to.equal(true);
	});

	it("Title does not start with invalid tokens d/W or r/W", () => {
		expect(verifyPRTitleForBadTitle("@W-12345678")).to.equal(true);
	});

	it("Branch is dev and title has version indicator", () => {
		expect(verifyPRTitleForBaseBranch("this title has the version indicator [2.x]", "dev")).to.equal(true);
	});

	it("Branch is not dev and title has version indicator", () => {
		expect(verifyPRTitleForBaseBranch("this title has version indicator [2.x]", "release")).to.equal(true);
	});

	it("Branch is not dev and title lacks version indicator", () => {
		expect(verifyPRTitleForBaseBranch("this title has version indicator [3.x]", "release")).to.equal(true);
	});
});

describe("Negative Tests", () => {
		it("work item out of minimum range", () => {
		expect(verifyPRTitleForBugId("@W-999@")).to.equal(false);
	});

	it("work item out of maximum range", () => {
		expect(verifyPRTitleForBugId("@W-000000000@")).to.equal(false);
	});

	it("work item missing leading @ sign", () => {
		expect(verifyPRTitleForBugId("W-1234@")).to.equal(false);
	});

	it("work item missing trailing @ sign", () => {
		expect(verifyPRTitleForBugId("@W-1234")).to.equal(false);
	});

	it("work item invalid format", () => {
		expect(verifyPRTitleForBugId("@W-1234ab@")).to.equal(false);
	});

	it("Title starts with invalid token d/W", () => {
		expect(verifyPRTitleForBadTitle("d/W")).to.equal(false);
	});

	it("Title starts with invalid token r/W", () => {
		expect(verifyPRTitleForBadTitle("r/W")).to.equal(false);
	});

	it("Branch is dev and title lacks version indicator", () => {
		expect(verifyPRTitleForBaseBranch("no version indicator here", "dev")).to.equal(false);
	});
});

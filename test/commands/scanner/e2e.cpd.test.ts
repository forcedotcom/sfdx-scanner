import { expect } from "chai";
import { runCommand } from "../../TestUtils";
import path = require("path");
import { ENGINE } from "../../../src/Constants";
import { RuleResult } from "../../../src/types";
import {CpdRuleCategory, CpdRuleName, CpdViolationSeverity } from "../../../src/lib/cpd/CpdEngine";

const Cpd_Test_Code_Path = path.join("test", "code-fixtures", "cpd");
const Vf_File1 = path.join(Cpd_Test_Code_Path, "myVfPage1.page");
const Vf_File2 = path.join(Cpd_Test_Code_Path, "myVfPage2.page");
const Apex_File1 = path.join(Cpd_Test_Code_Path, "SomeApex1.cls");
const Apex_File2 = path.join(Cpd_Test_Code_Path, "SomeApex2.cls");

const MINIMUM_TOKENS_ENV_VAR = 'SFDX_SCANNER_CPD_MINIMUM_TOKENS';

describe("End to end tests for CPD engine", () => {

	describe("Integration with `scanner run` command", () => {
		describe("Invoking CPD engine", () => {
			it("CPD engine should not be invoked by default", () => {
				const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path}`);
				assertNoError(output)
				expect(output.shellOutput.stdout).to.not.contain("Executed cpd");
			});

			it("CPD engine should be invocable using --engine flag", () => {
				const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd`);
				assertNoError(output)
				expect(output.shellOutput.stdout).to.contain("Executed cpd");
			});
		});

		it("Produces correct results in simple case", () => {
			const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
			assertNoError(output)
			const ruleResults: RuleResult[] = extractRuleResults(output.shellOutput);

			// Verify number of results.
			expect(ruleResults).to.have.lengthOf(4);
			// Verify that each result is well-formed.
			expect(ruleResults[0].engine).to.equal(ENGINE.CPD);
			expect(ruleResults[0].fileName).to.not.be.empty;
			expect(ruleResults[1].engine).to.equal(ENGINE.CPD);
			expect(ruleResults[1].fileName).to.not.be.empty;

			// Verify that the violations are well-formed and consistent.
			expect(ruleResults[0].violations).to.have.lengthOf(1);
			expect(ruleResults[0].violations[0].ruleName).to.equal(CpdRuleName);
			expect(ruleResults[0].violations[0].category).to.equal(CpdRuleCategory);
			expect(ruleResults[0].violations[0].severity).to.equal(CpdViolationSeverity);
			expect(ruleResults[1].violations).to.have.lengthOf(1);
			expect(ruleResults[1].violations[0].ruleName).to.equal(CpdRuleName);
			expect(ruleResults[1].violations[0].category).to.equal(CpdRuleCategory);
			expect(ruleResults[1].violations[0].severity).to.equal(CpdViolationSeverity);

			// Verify that the violation messages are well-formed.
			const violationMsg1 = ruleResults[0].violations[0].message;
			const violationMsg2 = ruleResults[1].violations[0].message;

			// confirm that the checksum is the same
			const checksum1 = violationMsg1.substring(0, violationMsg1.indexOf(":"));
			const checksum2 = violationMsg2.substring(0, violationMsg2.indexOf(":"));
			expect(checksum2).equals(checksum1);

			// confirm lines, tokens identified, and total counts are the same
			const lineAndToken1 = violationMsg1.replace("1 of 2", "# of 2");
			const lineAndToken2 = violationMsg2.replace("2 of 2", "# of 2");
			expect(lineAndToken2).equals(lineAndToken1);
		});

		describe("Processing Minimum Tokens value", () => {
			afterEach(() => {
				delete process.env[MINIMUM_TOKENS_ENV_VAR];
			});

			describe("Pulls value from environment variable if it is...", () => {
				it("...a wholly numeric string", () => {
					process.env[MINIMUM_TOKENS_ENV_VAR] = '200';
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					assertNoError(output)
					verifyEnvVarIsUsedForMinimumTokens(output.shellOutput);
				});

				it("...a partly numeric string", () => {
					// The environment variable processing will strip non-numeric characters, making this "600".
					process.env[MINIMUM_TOKENS_ENV_VAR] = 'My2String00';
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					verifyEnvVarIsUsedForMinimumTokens(output.shellOutput);
				});
			});

			describe("Pulls value from config if environment variable is...", () => {
				it("...undefined", () => {
					delete process.env[MINIMUM_TOKENS_ENV_VAR];
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					verifyDefaultConfigIsUsedForMinimumTokens(output.shellOutput);
				});

				it("...a wholly non-numeric string", () => {
					process.env[MINIMUM_TOKENS_ENV_VAR] = 'Some String';
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					verifyDefaultConfigIsUsedForMinimumTokens(output.shellOutput);
				});

				it('...an empty string', () => {
					process.env[MINIMUM_TOKENS_ENV_VAR] = '';
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					verifyDefaultConfigIsUsedForMinimumTokens(output.shellOutput);
				});
			});
		});
	});
});
function verifyEnvVarIsUsedForMinimumTokens(ctx) {
	const Minimum_Tokens_200 = [Vf_File1, Vf_File2].sort();
	const ruleResults: RuleResult[] = extractRuleResults(ctx);

	const actualFileNames = ruleResults.map(ruleResult => ruleResult.fileName).sort();
	expect(ruleResults.length).equals(Minimum_Tokens_200.length);

	for (let i = 0; i < ruleResults.length; i++) {
		// Comparing substring since actualFileName contains full path and Minimum_Tokens_300 contains relative paths
		expect(actualFileNames[i]).contains(Minimum_Tokens_200[i]);
	}
}

function verifyDefaultConfigIsUsedForMinimumTokens(ctx) {
	const Minimum_Tokens_100 = [Apex_File1, Apex_File2, Vf_File1, Vf_File2].sort();

	const ruleResults: RuleResult[] = extractRuleResults(ctx);

	const actualFileNames = ruleResults.map(ruleResult => ruleResult.fileName).sort();
	expect(ruleResults.length).equals(Minimum_Tokens_100.length);

	for (let i = 0; i < ruleResults.length; i++) {
		// Comparing substring since actualFileName contains full path and Minimum_Tokens_100 contains relative paths
		expect(actualFileNames[i]).contains(Minimum_Tokens_100[i]);
	}
}

function extractRuleResults(ctx) {
	const stdout = ctx.stdout;
	const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
	expect(jsonOutput).is.not.empty;
	const ruleResults: RuleResult[] = JSON.parse(jsonOutput);
	return ruleResults;
}

function assertNoError(output) {
	if (output.shellOutput.stderr.includes("Error")) {
		expect.fail("Found error in stderr output:\n" + output.shellOutput.stderr);
	}
}


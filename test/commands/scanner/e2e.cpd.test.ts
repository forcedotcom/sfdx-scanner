import { expect } from "@salesforce/command/lib/test";
// @ts-ignore
import { runCommand } from "../../TestUtils";
import path = require("path");
import { ENGINE } from "../../../src/Constants";
import { RuleResult } from "../../../src/types";
import { CpdLanguagesSupported, CpdRuleCategory, CpdRuleDescription, CpdRuleName, CpdViolationSeverity } from "../../../src/lib/cpd/CpdEngine";

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
				expect(output.shellOutput.stdout).to.not.contain("Executed cpd");
			});

			it("CPD engine should be invocable using --engine flag", () => {
				const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd`);
				expect(output.shellOutput.stdout).to.contain("Executed cpd");
			});
		});

		it("Produces correct results in simple case", () => {
			const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
			const ruleResults: RuleResult[] = extractRuleResults(output.shellOutput);

			// Verify number of results.
			expect(ruleResults).to.have.lengthOf(2);
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
			const checksum1 = violationMsg1.substr(0, violationMsg1.indexOf(":"));
			const checksum2 = violationMsg2.substr(0, violationMsg2.indexOf(":"));
			expect(checksum2).equals(checksum1);

			// confirm lines and tokens identified are the same
			const lineAndToken1 = violationMsg1.substr(violationMsg1.indexOf("detected."));
			const lineAndToken2 = violationMsg2.substr(violationMsg2.indexOf("detected."));
			expect(lineAndToken2).equals(lineAndToken1);

			// confirm total count of duplications
			const totalCount1 = violationMsg1.substr(violationMsg1.indexOf("of "), violationMsg1.indexOf(" duplication"));
			const totalCount2 = violationMsg2.substr(violationMsg2.indexOf("of "), violationMsg2.indexOf(" duplication"));
			expect(totalCount2).equals(totalCount1);
		});

		describe("Processing Minimum Tokens value", () => {
			afterEach(() => {
				delete process.env[MINIMUM_TOKENS_ENV_VAR];
			});

			describe("Pulls value from environment variable if it is...", () => {
				it("...a wholly numeric string", () => {
					process.env[MINIMUM_TOKENS_ENV_VAR] = '50';
					const output = runCommand(`scanner run --target ${Cpd_Test_Code_Path} --engine cpd --format json`);
					verifyEnvVarIsUsedForMinimumTokens(output.shellOutput);
				});

				it("...a partly numeric string", () => {
					// The environment variable processing will strip non-numeric characters, making this "50".
					process.env[MINIMUM_TOKENS_ENV_VAR] = 'My5String0';
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

	describe("Integration with `scanner rule list` command", () => {
			describe("Invoking CPD engine", () => {
				it("CPD engine rules should not be displayed by default", () => {
					const output = runCommand(`scanner rule list --json`);
					const results = output.jsonOutput.result as any[];
					expect(results.length).to.be.greaterThan(0);

					const cpdCatalogs = results.filter(row => row.engine === ENGINE.CPD);
					expect(cpdCatalogs).to.have.lengthOf(0);
				});

				it("CPD engine rules should be displayed when using `--engine cpd`", () => {
					const output = runCommand(`scanner rule list --engine cpd --json`);
					const results = output.jsonOutput.result as any[];
					expect(results.length).equals(1);

					// Verify properties of rule.
					const rule = results[0];
					expect(rule.engine).equals(ENGINE.CPD);
					expect(rule.sourcepackage).equals(ENGINE.CPD);
					expect(rule.name).equals(CpdRuleName);
					expect(rule.description).equals(CpdRuleDescription);
					expect(rule.categories).contains(CpdRuleCategory);
					expect(rule.languages).has.same.members(CpdLanguagesSupported);
					expect(rule.defaultEnabled).equals(true);
				});
			});
	});
});
function verifyEnvVarIsUsedForMinimumTokens(ctx) {
	const Minimum_Tokens_50 = [Apex_File1, Apex_File2, Vf_File1, Vf_File2].sort();
	const ruleResults: RuleResult[] = extractRuleResults(ctx);

	const actualFileNames = ruleResults.map(ruleResult => ruleResult.fileName).sort();
	expect(ruleResults.length).equals(Minimum_Tokens_50.length);

	for (let i = 0; i < ruleResults.length; i++) {
		// Comparing substring since actualFileName contains full path and Minimum_Tokens_50 contains relative paths
		expect(actualFileNames[i]).contains(Minimum_Tokens_50[i]);
	}
}

function verifyDefaultConfigIsUsedForMinimumTokens(ctx) {
	const Minimum_Tokens_100 = [Vf_File1, Vf_File2].sort();

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


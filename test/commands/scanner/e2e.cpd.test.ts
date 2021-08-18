import { expect } from "@salesforce/command/lib/test";
import { setupCommandTest } from "../../TestUtils";
import path = require("path");
import { ENGINE } from "../../../src/Constants";
import { RuleResult } from "../../../src/types";
import { CpdLanguagesSupported, CpdRuleCategory, CpdRuleDescription, CpdRuleName, CpdViolationSeverity } from "../../../src/lib/cpd/CpdEngine";

const Cpd_Test_Code_Path = path.join("test", "code-fixtures", "cpd");
const Vf_File1 = path.join(Cpd_Test_Code_Path, "myVfPage1.page");
const Vf_File2 = path.join(Cpd_Test_Code_Path, "myVfPage2.page");
const Apex_File1 = path.join(Cpd_Test_Code_Path, "SomeApex1.cls");
const Apex_File2 = path.join(Cpd_Test_Code_Path, "SomeApex2.cls");

describe("End to end tests for CPD engine", () => {

	describe("scanner:run", () => {
		describe("Invoking CPD engine", () => {
			setupCommandTest
				.command([
					"scanner:run",
					"--target", Cpd_Test_Code_Path,
				])
				.it("CPD engine should not be invoked by default", (ctx) => {
					expect(ctx.stdout).to.not.contain("Executed cpd");
				});

			setupCommandTest
				.command([
					"scanner:run",
					"--target", Cpd_Test_Code_Path,
					"--engine", "cpd",
				])
				.it("CPD engine should be invoked using --engine", (ctx) => {
					expect(ctx.stdout).to.contain("Executed cpd");
				});
		});

		describe("CPD execution", () => {
			setupCommandTest
				.command([
					"scanner:run",
					"--target", Cpd_Test_Code_Path,
					"--engine", ENGINE.CPD,
					"--format", "json"
				])
				.it("Verify CPD results", (ctx) => {
					const ruleResults: RuleResult[] = extractRuleResults(ctx);

					//verify rule results
					expect(ruleResults.length).greaterThan(0);
					for (const ruleResult of ruleResults) {
						expect(ruleResult.engine).equals(ENGINE.CPD);
						expect(ruleResult.fileName).is.not.empty;
						expect(ruleResult.violations).is.not.empty;

						//verify rule violations
						for (const violation of ruleResult.violations) {
							expect(violation.ruleName).equals(CpdRuleName);
							expect(violation.category).equals(CpdRuleCategory);
							expect(violation.severity).equals(CpdViolationSeverity);
						}
					}

				});

		});

		describe("Processing Minimum Tokens value", () => {

			describe("Minimum tokens value from env", () => {
				before(() => {
					process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'] = '50';
				});

				after(() => {
					delete process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'];
				});

				setupCommandTest
					.command([
						"scanner:run",
						"--target", Cpd_Test_Code_Path,
						"--engine", ENGINE.CPD,
						"--format", "json"
					])
					.it("Picks up minimum tokens from Environment variable when found", ctx => {
						verifyEnvVarIsUsedForMinimumTokens(ctx);
					});
			});

			describe("Minimum tokens value from config", () => {

				before(() => {
					// delete property if it exists
					delete process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'];
				});

				setupCommandTest
					.command([
						"scanner:run",
						"--target", Cpd_Test_Code_Path,
						"--engine", ENGINE.CPD,
						"--format", "json"
					])
					.it("Picks up minimum tokens from Config when Environment variable is not found", ctx => {
						verifyDefaultConfigIsUsedForMinimumTokens(ctx);
					});
			});

			describe("Invalid String Minimum tokens value from env", () => {
				before(() => {
					process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'] = 'Some String';
				});

				after(() => {
					delete process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'];
				});

				setupCommandTest
					.command([
						"scanner:run",
						"--target", Cpd_Test_Code_Path,
						"--engine", ENGINE.CPD,
						"--format", "json"
					])
					.it("Uses config value when environment variable is not a number", ctx => {
						verifyDefaultConfigIsUsedForMinimumTokens(ctx);
					});
			});

			describe("Invalid empty Minimum tokens value from env", () => {
				before(() => {
					process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'] = '';
				});

				after(() => {
					delete process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'];
				});

				setupCommandTest
					.command([
						"scanner:run",
						"--target", Cpd_Test_Code_Path,
						"--engine", ENGINE.CPD,
						"--format", "json"
					])
					.it("Uses config value when environment variable is not empty", ctx => {
						verifyDefaultConfigIsUsedForMinimumTokens(ctx);
					});
			});

			describe("String and digit Minimum tokens value from env", () => {
				before(() => {
					process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'] = 'My5String0';
				});

				after(() => {
					delete process.env['SFDX_SCANNER_CPD_MINIMUM_TOKENS'];
				});

				setupCommandTest
					.command([
						"scanner:run",
						"--target", Cpd_Test_Code_Path,
						"--engine", ENGINE.CPD,
						"--format", "json"
					])
					.it("Uses config value when environment variable is not a number", ctx => {
						verifyEnvVarIsUsedForMinimumTokens(ctx);
					});
			});

		});

		describe("Violation message content", () => {
			setupCommandTest
				.command([
					"scanner:run",
					"--target", Cpd_Test_Code_Path,
					"--engine", ENGINE.CPD,
					"--format", "json"
				])
				.it("Has expected violation message", (ctx) => {
					const ruleResults = extractRuleResults(ctx);

					// for default 100 minimum tokens, we should have one duplication with two entries
					expect(ruleResults.length).equals(2);
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
		});
	});

	describe("scanner:rule:list", () => {
			describe("Invoking CPD engine", () => {
				setupCommandTest
					.command([
						"scanner:rule:list",
						"--json"
					])
					.it("CPD engine should not be listed by default", (ctx) => {
						const output = JSON.parse(ctx.stdout);
						const results = output.result;
						expect(results.length).greaterThan(0);

						const cpdCatalogs = results.filter(row => row.engine == ENGINE.CPD);
						expect(cpdCatalogs.length).equals(0);
					});
	
				setupCommandTest
					.command([
						"scanner:rule:list",
						"--engine", "cpd",
						"--json"
					])
					.it("CPD engine should be listed when using --engine", (ctx) => {
						const output = JSON.parse(ctx.stdout);
						const results = output.result;
						expect(results.length).equals(1);

						//verify contents of Rule
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


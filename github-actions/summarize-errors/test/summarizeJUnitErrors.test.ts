import path = require('path');
import { expect } from "chai";
import {ClassDescriptor} from "../src/types";
import {summarizeErrors} from "../src/summarizeJUnitErrors";

describe("#summarizeErrors", () => {
	it('When there are no failures, does nothing', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'no-failures'));
		const summary: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(0);
	});

	it('If one file has failures, one summary is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'one-failing-file'));
		const summary: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(1);
		expect(summary[0].file).to.equal("classes/com.salesforce.rules.RuleRunnerTest.html");
		expect(summary[0].failures.length).to.equal(1);
		expect(summary[0].failures[0].test).to.equal("properlyRunsStaticRule()");
	});

	it('When multiple files have failures, one summary per file is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'multiple-failing-files'));
		const summary: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(2);
		expect(summary[0].file).to.equal("classes/com.salesforce.rules.RuleRunnerTest.html");
		expect(summary[0].failures.length).to.equal(1);
		expect(summary[0].failures[0].test).to.equal("properlyRunsStaticRule()");
		expect(summary[1].file).to.equal("classes/com.salesforce.rules.fls.apex.CheckBasedFieldLevelFlsViolationTest.html");
		expect(summary[1].failures.length).to.equal(3);
		expect(summary[1].failures[0].test).to.equal("testObjectLevelCheckIsntAccepted(FlsValidationType, String, AbstractPathBasedRule, String): READ");
		expect(summary[1].failures[1].test).to.equal("testObjectLevelCheckIsntAccepted(FlsValidationType, String, AbstractPathBasedRule, String): INSERT");
		expect(summary[1].failures[2].test).to.equal("testObjectLevelCheckIsntAccepted(FlsValidationType, String, AbstractPathBasedRule, String): UPDATE");
	});
});

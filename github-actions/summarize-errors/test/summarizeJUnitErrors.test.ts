import path = require('path');
import { expect } from "chai";
import {summarizeErrors} from "../lib/summarizeJUnitErrors";

describe("#summarizeErrors", () => {
	it('When there are no failures, does nothing', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'no-failures'));
		const summary: string[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(0);
	});

	it('If one file has failures, one summary is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'one-failing-file'));
		const summary: string[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(1);
		expect(summary[0]).to.contain("properlyRunsStaticRule()");
	});

	it('When multiple files have failures, one summary per file is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'multiple-failing-files'));
		const summary: string[] = await summarizeErrors(pathToProject);
		expect(summary.length).to.equal(2);
		expect(summary[0]).to.contain("properlyRunsStaticRule()");
		expect(summary[1]).to.contain("testObjectLevelCheckIsntAccepted(FlsValidationType, String, AbstractPathBasedRule, String): READ");
		expect(summary[1]).to.contain("testObjectLevelCheckIsntAccepted(FlsValidationType, String, AbstractPathBasedRule, String): UPDATE");
	});
});

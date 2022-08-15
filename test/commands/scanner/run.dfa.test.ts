import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import * as path from 'path';
import Dfa from '../../../src/commands/scanner/run/dfa';
import * as sinon from 'sinon';


Messages.importMessagesDirectory(__dirname);
const sfgeMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');

const dfaTarget = path.join('test', 'code-fixtures', 'projects', 'sfge-smoke-app', 'src');
const projectdir = path.join('test', 'code-fixtures', 'projects', 'sfge-smoke-app', 'src');

const apexControllerStr = 'UnsafeVfController';
const customSettingsStr = 'none found';
const fileCount = '7';
const entryPointCount = '5';
const pathCount = '6';
const violationCount = '2';

const customSettingsMessage = sfgeMessages.getMessage('info.sfgeMetaInfoCollected', ['Custom Settings', customSettingsStr]);
const apexControllerMessage = sfgeMessages.getMessage('info.sfgeMetaInfoCollected', ['Apex Controllers', apexControllerStr]);
const compiledMessage = sfgeMessages.getMessage('info.sfgeFinishedCompilingFiles', [fileCount]);
const startGraphBuildMessage = sfgeMessages.getMessage('info.sfgeStartedBuildingGraph');
const endGraphBuildMessage = sfgeMessages.getMessage('info.sfgeFinishedBuildingGraph');
const identifiedEntryMessage = sfgeMessages.getMessage('info.sfgePathEntryPointsIdentified', [entryPointCount]);
const completedAnalysisMessage = sfgeMessages.getMessage('info.sfgeCompletedPathAnalysis', [pathCount, entryPointCount, violationCount]);

function isSubstr(output: string, substring: string): boolean {
	const updatedSubstr = substring.replace('[', '\\[');
	const regex = new RegExp(`^${updatedSubstr}`, 'gm');
	const regexMatch = output.match(regex);
	return regexMatch != null && regexMatch.length >= 1;
}

function verifyContains(output: string, substring: string): void {
	expect(isSubstr(output, substring), `Output "${output}" should contain substring "${substring}"`).is.true;
}

function verifyNotContains(output: string, substring: string): void {
	expect(isSubstr(output, substring), `Output "${output}" should not contain substring "${substring}"`).is.false;
}

describe('scanner:run:dfa', function () {
	this.timeout(10000); // TODO why do we get timeouts at the default of 5000?  What is so expensive here?

	describe('End to end', () => {

		describe('Progress output', () => {
			let sandbox;
			let spy: sinon.SinonSpy;
				before(() => {
					sandbox = sinon.createSandbox();
					spy = sandbox.spy(Dfa.prototype, "updateSpinner");
				});
	
				after(() => {
					spy.restore();
					sinon.restore();
				});

			setupCommandTest
				.command(['scanner:run:dfa',
					'--target', dfaTarget,
					'--projectdir', projectdir,
					'--format', 'json'
				])
				.it('contains valid information when --verbose is not used', ctx => {
					const output = ctx.stdout;
					verifyNotContains(output, customSettingsMessage);
					verifyNotContains(output, apexControllerMessage);
					expect(spy.calledWith(compiledMessage, startGraphBuildMessage, endGraphBuildMessage, identifiedEntryMessage, completedAnalysisMessage));
				});

			setupCommandTest
				.command(['scanner:run:dfa',
					'--target', dfaTarget,
					'--projectdir', projectdir,
					'--format', 'json',
					'--verbose'
				])
				.it('contains valid information with --verbose', ctx => {
					const output = ctx.stdout;
					verifyContains(output, customSettingsMessage);
					verifyContains(output, apexControllerMessage);
					expect(spy.calledWith(compiledMessage, startGraphBuildMessage, endGraphBuildMessage, identifiedEntryMessage, completedAnalysisMessage));
				});
		});

		describe('Output consistency', () => {
			describe('run with format --json', () => {
				setupCommandTest
				.command(['scanner:run:dfa',
				'--target', dfaTarget,
				'--projectdir', projectdir,
				'--format', 'json'
			])
			.it('provides only json in stdout', ctx => {
				try {
					JSON.parse(ctx.stdout);
				} catch (error) {
					expect.fail("dummy", "another dummy", "Invalid JSON output from --format json: " + ctx.stdout + ", error = " + error);
				}
				
				});
			});
		});
		
	});
});
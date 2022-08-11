import {expect} from '@salesforce/command/lib/test';
import {setupCommandTest} from '../../TestUtils';
import {Messages} from '@salesforce/core';
import * as path from 'path';


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
	const regex = new RegExp(`^${substring}`, 'gm');
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
	describe('End to end', () => {
		describe('--verbose', () => {
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
					verifyNotContains(output, compiledMessage);
					verifyNotContains(output, startGraphBuildMessage);
					verifyNotContains(output, endGraphBuildMessage);
					verifyNotContains(output, identifiedEntryMessage);
					verifyContains(output, completedAnalysisMessage);
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
					verifyContains(output, compiledMessage);
					verifyContains(output, startGraphBuildMessage);
					verifyContains(output, endGraphBuildMessage);
					verifyContains(output, identifiedEntryMessage);
					verifyContains(output, completedAnalysisMessage);
				});
		});
	});
});
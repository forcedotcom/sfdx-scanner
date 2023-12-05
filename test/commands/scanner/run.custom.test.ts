import { Messages } from "@salesforce/core";
// @ts-ignore
import { runCommand } from "../../TestUtils";
import path = require('path');
import { expect } from 'chai';
import {ENGINE} from '../../../src/Constants';
import normalize = require('normalize-path');


Messages.importMessagesDirectory(__dirname);
const eventMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');


describe('scanner:run with custom config E2E', () => {
	const customPmdConfig =  path.join('.', 'test', 'code-fixtures', 'config', 'pmd_custom_config.xml');
	const customEslintConfig = path.join('test', 'code-fixtures', 'config', 'custom_eslint_config.json');

	it('Can use custom PMD config to detect violations', () => {
		const targetPath = path.join('.', 'test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'pages', 'testSELECT2.page');
		const output = runCommand(`scanner run --target ${targetPath} --pmdconfig ${customPmdConfig} --format json`);
		const stdout = output.shellOutput.stdout;
		expect(stdout).to.not.be.empty;

		// Verify that the expected warning is displayed.
		const expectedMessage = eventMessages.getMessage('info.customPmdHeadsUp', [normalize(customPmdConfig)]);
		expect(stdout).to.contain(expectedMessage);

		// Verify that the contents are correct.
		const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
		expect(jsonOutput).to.not.be.empty;
		const parsedJson = JSON.parse(jsonOutput);
		expect(parsedJson).to.have.lengthOf(1);
		expect(parsedJson[0].engine).to.equal(ENGINE.PMD_CUSTOM.valueOf());
		expect(parsedJson[0].violations).to.have.lengthOf(1);
	});

	it('Can use custom ESLint config to detect violations', () => {
		const targetPath = path.join('.', 'test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts');
		const output = runCommand(`scanner run --target ${targetPath} --eslintconfig ${customEslintConfig} --format json`);
		const stdout = output.shellOutput.stdout;
		expect(stdout).to.not.be.empty;

		// Verify that the expected warning is displayed.
		const expectedMessage = eventMessages.getMessage('info.customEslintHeadsUp', [normalize(customEslintConfig)]);
		expect(stdout).to.contain(expectedMessage);

		// Verify that the contents are correct.
		const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
		expect(jsonOutput).to.not.be.empty;
		const parsedJson = JSON.parse(jsonOutput);
		expect(parsedJson).to.have.lengthOf(1);
		expect(parsedJson[0].engine).to.equal(ENGINE.ESLINT_CUSTOM.valueOf());
		expect(parsedJson[0].violations.length).to.equal(1);
	});

	it('Default engine and custom variant are mutually exclusive', () => {
		const targetPath = path.join('.', 'test', 'code-fixtures', 'projects', 'app', 'force-app');
		const output = runCommand(`scanner run --target ${targetPath} --pmdconfig ${customPmdConfig} --format json`);
		const stdout = output.shellOutput.stdout;
		const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
		expect(jsonOutput).to.not.be.empty;

		const parsedOutput = JSON.parse(jsonOutput);

		const onlyCustomPmdAndDefaultEslint = parsedOutput.filter(violation => {
			return (violation.engine === ENGINE.PMD_CUSTOM.valueOf() || violation.engine === ENGINE.ESLINT.valueOf());
		});

		expect(parsedOutput.length).to.equal(onlyCustomPmdAndDefaultEslint.length, 'Violations should be only from Custom PMD and Default ESLint');
	});
});

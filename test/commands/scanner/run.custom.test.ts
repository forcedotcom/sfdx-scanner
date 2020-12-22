import { Messages } from "@salesforce/core";
import { setupCommandTest } from "../../TestUtils";
import path = require('path');
import { expect } from "@oclif/test";
import {ENGINE} from '../../../src/Constants';
import normalize = require('normalize-path');


Messages.importMessagesDirectory(__dirname);
const eventMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');


describe('scanner:run with custom config E2E', () => {
	const customPmdConfig =  path.join('.', 'test', 'code-fixtures', 'config', 'pmd_custom_config.xml');
	const customEslintConfig = path.join('test', 'code-fixtures', 'config', 'custom_eslint_config.json');


	describe('Custom PMD config', () => {

		
		setupCommandTest
		.command(['scanner:run', 
			'--target', path.join('.', 'test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'pages', 'testSELECT2.page'),
			'--pmdconfig', customPmdConfig,
			'--format', 'json'])
		.it('should use custom pmd config to detect violations', (ctx) => {
			const stdout = ctx.stdout;
			const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
			expect(jsonOutput).to.be.not.empty;
			const output = JSON.parse(jsonOutput);

			//verify rule violations
			expect(output.length).to.equal(1);
			expect(output[0].engine).to.equal(ENGINE.PMD_CUSTOM.valueOf());
			expect(output[0].violations.length).to.equal(1);
			
		});

		setupCommandTest
		.command(['scanner:run', 
			'--target', path.join('.', 'test', 'code-fixtures', 'projects', 'app', 'force-app', 'main', 'default', 'pages', 'testSELECT2.page'),
			'--pmdconfig', customPmdConfig])
		.it('should display warning that we are about to run PMD with custom config', (ctx) => {
			const stdout = ctx.stdout;			
			const expectedMessage = eventMessages.getMessage('info.customPmdHeadsUp', [normalize(customPmdConfig)]);

			expect(stdout).contains(expectedMessage);
		});

	});

	describe('Custom Eslint config', () => {

		setupCommandTest
		.command(['scanner:run',
			'--target', path.join('.', 'test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts'),
			'--eslintconfig', customEslintConfig,
			'--format', 'json'])
		.it('should use custom eslint config to detect violations', ctx => {
			const stdout = ctx.stdout;
			const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
			expect(jsonOutput).to.be.not.empty;
			const output = JSON.parse(jsonOutput);

			//verify rule violations
			expect(output.length).to.equal(1);
			expect(output[0].engine).to.equal(ENGINE.ESLINT_CUSTOM.valueOf());
			expect(output[0].violations.length).to.equal(1);
		});


		setupCommandTest
		.command(['scanner:run',
			'--target', path.join('.', 'test', 'code-fixtures', 'projects', 'ts', 'src', 'simpleYetWrong.ts'),
			'--eslintconfig', customEslintConfig])
		.it('should display warning that we are about to run Eslint with custom config', (ctx) => {
			const stdout = ctx.stdout;			
			const expectedMessage = eventMessages.getMessage('info.customEslintHeadsUp', [normalize(customEslintConfig)]);
							
			expect(stdout).contains(expectedMessage);
		});
	});

	describe('Engine exclusivity with custom config', () => {

		setupCommandTest
		.command(['scanner:run',
			'--target', path.join('.', 'test', 'code-fixtures', 'projects', 'app', 'force-app'),
			'--pmdconfig', customPmdConfig,
			'--format', 'json'])
		.it('should not run default PMD engine when custom config provided, but can run default Eslint engines', ctx => {
			const stdout = ctx.stdout;
			const jsonOutput = stdout.slice(stdout.indexOf('['), stdout.lastIndexOf(']') + 1);
			expect(jsonOutput).to.be.not.empty;

			const output = JSON.parse(jsonOutput);

			const onlyCustomPmdAndDefaultEslint = output.filter(violation => {
				return (violation.engine === ENGINE.PMD_CUSTOM.valueOf() || violation.engine === ENGINE.ESLINT.valueOf());
			});

			expect(output.length).equals(onlyCustomPmdAndDefaultEslint.length, 'Rule violations should include violations from custom PMD and default Eslint');
		});
	});
});

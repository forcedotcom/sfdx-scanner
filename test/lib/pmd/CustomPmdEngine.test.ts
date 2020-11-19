import 'reflect-metadata';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {expect} from 'chai';
import Sinon = require('sinon');
import {CustomPmdEngine}  from '../../../src/lib/pmd/PmdEngine'
import { CUSTOM_CONFIG } from '../../../src/Constants';
import * as DataGenerator from '../eslint/EslintTestDataGenerator';
import { Messages } from '@salesforce/core';

const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'PmdEngine');

const configFilePath = '/some/file/path/rule-ref.xml';
const engineOptionsWithPmdCustom = new Map<string, string>([
	[CUSTOM_CONFIG.PmdConfig, configFilePath]
]);

const emptyEngineOptions = new Map<string, string>();

const engineOptionsWithEslintCustom = new Map<string, string>([
	[CUSTOM_CONFIG.EslintConfig, configFilePath]
]);

describe('Tests for CustomPmdEngine implementation', () => {

	describe('shouldEngineRun() for CustomPmdEngine', () => {
		const engine = new CustomPmdEngine();

		before(async () => {
			await engine.init();
		});

		it('should decide to run when engineOptions map contains pmdconfig', () => {
			const shouldEngineRun = engine.shouldEngineRun(
				[],
				[],
				[],
				engineOptionsWithPmdCustom
			);

			expect(shouldEngineRun).to.be.true;
		});

		it('should decide to NOT run when engineOptions map does not contain pmdconfig', () => {
			const shouldEngineRun = engine.shouldEngineRun(
				[],
				[],
				[],
				emptyEngineOptions
			);

			expect(shouldEngineRun).to.be.false;
		});

		it('should decide to NOT run when engineOptions map does not contain pmdconfig but only Eslint config', () => {
			const shouldEngineRun = engine.shouldEngineRun(
				[],
				[],
				[],
				engineOptionsWithEslintCustom
			);

			expect(shouldEngineRun).to.be.false;
		});
	});

	describe('Tests for isEngineRequested()', () => {
		const engine = new CustomPmdEngine();

		before(async () => {
			await engine.init();
		});

		it('should return true when custom config is present and filter contains "pmd"', () => {
			const filteredNames = ['eslint-lwc', 'pmd', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false when custom config is present but filter does not contain "pmd"', () => {
			const filteredNames = ['eslint-lwc', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.false;		
		});

		it('should return false when custom config is not present even if filter contains "pmd"', () => {
			const filteredNames = ['eslint-lwc', 'pmd', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, emptyEngineOptions);

			expect(isEngineRequested).to.be.false;		
		});

		it('should return true when custom config is present and filter contains a string that starts with "pmd"', () => {
			const filteredNames = ['eslint-lwc', 'pmd-custom', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithPmdCustom);

			expect(isEngineRequested).to.be.true;
		});

		it('should return false when custom config for eslint is present and filter contains "pmd"', () => {
			const filteredNames = ['eslint-lwc', 'pmd', 'retire-js'];

			const isEngineRequested = engine.isEngineRequested(filteredNames, engineOptionsWithEslintCustom);

			expect(isEngineRequested).to.be.false;		
		});
	});

	describe('tests for run()', () => {
		const engine = new CustomPmdEngine();

		before(async () => {
			Sinon.createSandbox();
			await engine.init();
		});
		after(() => {
			Sinon.restore();
		});
		it('should throw error when Config file does not exist', () => {
			Sinon.stub(FileHandler.prototype, 'exists').resolves(false);

			try {
				engine.run(
					[DataGenerator.getDummyRuleGroup()],
					[],
					[DataGenerator.getDummyTarget()],
					engineOptionsWithPmdCustom
				);
				//TODO: fail when no error is thrown
			} catch (error) {
				expect(error.message).equals(messages.getMessage('ConfigNotFound', [configFilePath]));
			}

		});
	});
});

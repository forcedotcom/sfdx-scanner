import { expect } from 'chai';
import Sinon = require('sinon');
import {TypescriptEslintStrategy} from '../../../src/lib/eslint/TypescriptEslintStrategy';
import { Config } from '../../../src/lib/util/Config';
import { FileHandler } from '../../../src/lib/util/FileHandler';
import { fail } from 'assert';
import * as path from 'path';
import {Messages} from '@salesforce/core';

const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'eslintEngine');

describe('TypescriptEslint Strategy', () => {
	describe('Test cases with #findTsConfig', () => {
		const invalidPath = '/invalid/path';
		const tsconfigFilePath = '/path/to/tsconfig.json';
		const tsconfigDir = '/path/where/ts/config/lives';
		const tsconfigFileInDir = `${tsconfigDir}/tsconfig.json`;
		const tsconfigInCwd = path.resolve('', 'tsconfig.json');

		describe('find tsconfig from Config.json', () => {
			const target = '';

			afterEach(() => {
				Sinon.restore();
			});

			it('should throw error if OverriddenConfigPath in Config.json contains an invalid path', async () => {
				Sinon.stub(Config.prototype, "getOverriddenConfigPath").returns(invalidPath);
				Sinon.stub(FileHandler.prototype, "exists").resolves(false);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				try {
					await tsStrategy.findTsconfig(target);
					fail("Invalid path in Config.json's OverriddenConfigPath should cause an error");
				} catch (error) {
					expect(error.message).equals(messages.getMessage('InvalidPath', [invalidPath]));
				}

			});

			it('should use tsconfig file from Config.json if OverriddenConfigPath is a valid file', async () => {
				Sinon.stub(Config.prototype, "getOverriddenConfigPath").returns(tsconfigFilePath);
				Sinon.stub(FileHandler.prototype, "exists").resolves(true);
				Sinon.stub(FileHandler.prototype, 'isDir').resolves(false);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.findTsconfig(target);

				expect(fileFound).equals(tsconfigFilePath);
			});

			it('should look for tsconfig file in directory given in Config.json if OverriddenConfigPath is a valid directory', async () => {
				Sinon.stub(Config.prototype, 'getOverriddenConfigPath'). returns(tsconfigDir);
				// exists() returns true for config path and tsconfig file inside
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'isDir').resolves(true);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.findTsconfig(target);

				expect(fileFound).equals(tsconfigFileInDir);
			});

		});

		describe('find tsconfig from target or current working directory', () => {
			const target = tsconfigDir;
			afterEach(() => {
				Sinon.restore();
			});

			it('should look for tsconfig in target directory if config has not been set', async () => {
				Sinon.stub(Config.prototype, "getOverriddenConfigPath").returns('');
				// exists() returns true for target path dir and tsconfig.json within it
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'isDir').resolves(true);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.findTsconfig(target);

				expect(fileFound).equals(tsconfigFileInDir);

			});

			it('should look for tsconfig in current working directory if target is not a directory', async () => {
				Sinon.stub(Config.prototype, "getOverriddenConfigPath").returns('');
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'isDir').resolves(false);

				const tsStrategy = new TypescriptEslintStrategy();
				await tsStrategy.init();

				const fileFound = await tsStrategy.findTsconfig(target);

				expect(fileFound).equals(tsconfigInCwd);
			});

			// TODO: test cases that set exists to true/false depending on call order
		});
	});
});
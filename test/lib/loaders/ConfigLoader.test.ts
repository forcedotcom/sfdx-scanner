import * as path from 'node:path';

import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {ConfigLoaderImpl} from '../../../src/lib/loaders/ConfigLoader';


describe('ConfigLoaderImpl', () => {
	it('When not provided a config file path, returns default config', () => {
		const loader = new ConfigLoaderImpl();
		const expectedConfig = CodeAnalyzerConfig.withDefaults();

		const testedConfig = loader.loadConfig();

		expect(testedConfig).toEqual(expectedConfig);
	});

	it('When provided path to a valid config file, returns corresponding config', () => {
		const loader = new ConfigLoaderImpl();
		const configPath = path.resolve('test', 'fixtures', 'valid-configs', 'sample-config-file.yml');
		const testedConfig = loader.loadConfig(configPath);
		expect(testedConfig.getRuleOverridesFor('stubEngine1')).toEqual({
			stub1RuleB: {
				severity: 1
			},
			stub1RuleD: {
				severity: 5,
				tags: ["Recommended", "CodeStyle", "Performance"]
			}
		});
		expect(testedConfig.getRuleOverridesFor('stubEngine2')).toEqual({
			stub2RuleA: {
				tags: ['Security', 'SomeNewTag']
			}
		});
	});

	it('When provided path to invalid config file, throws helpful error', () => {
		const loader = new ConfigLoaderImpl();
		// Specify a config file that specifies a non-existent log_folder property.
		const configPath = path.resolve('test', 'fixtures', 'invalid-configs', 'nonexistent-log-folder.yml');
		// Attempt to load the config, and assert that the error message mentions the bad log folder.
		// From that, we can reasonably surmise that the log folder was the cause of the error, and be satisfied
		// that the user was informed of this problem.
		expect(() => loader.loadConfig(configPath)).toThrow('nonExistentLogFolder');
	});
});

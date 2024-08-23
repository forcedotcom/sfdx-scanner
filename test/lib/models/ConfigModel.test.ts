import path from 'node:path';
import * as fsp from 'node:fs/promises';
import {CodeAnalyzerConfig, CodeAnalyzer, RuleSelection} from '@salesforce/code-analyzer-core';

import {AnnotatedConfigModel, OutputFormat} from '../../../src/lib/models/ConfigModel';

import {FunctionalStubEnginePlugin1} from '../../stubs/StubEnginePlugins';

describe('ConfigModel implementations', () => {
	describe('AnnotatedConfigModel', () => {

		const PATH_TO_COMPARISON_DIR = path.resolve(__dirname, '..', '..', 'fixtures', 'comparison-files', 'lib', 'models', 'ConfigModel.test.ts');
		const PATH_TO_PROJECT_ROOT = path.resolve(PATH_TO_COMPARISON_DIR, '..', '..', '..', '..', '..', '..');
		let inputConfig: CodeAnalyzerConfig;
		let core: CodeAnalyzer;

		beforeAll(async () => {
			// Read the input file as a string.
			let inputConfigString: string = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'input-config.yml'), {encoding: 'utf-8'});
			// Process it so it's usable as a config.
			inputConfigString = inputConfigString
				// The config root should be the file's parent directory.
				.replaceAll('__DUMMY_CONFIG_ROOT__', PATH_TO_COMPARISON_DIR)
				// The log folder should be a relative path back up to the project root.
				.replaceAll('__DUMMY_LOG_FOLDER__', PATH_TO_PROJECT_ROOT);
			inputConfig = CodeAnalyzerConfig.fromYamlString(inputConfigString);
			core = new CodeAnalyzer(inputConfig);
			await core.addEnginePlugin(new FunctionalStubEnginePlugin1());
		});

		describe('Conversion into YAML', () => {
			describe('Top-level properties', () => {

				let annotatedConfigModel: AnnotatedConfigModel;

				beforeAll(async () => {
					// Just select all rules for these tests, since they don't concern rule resolution.
					const ruleSelection: RuleSelection = await core.selectRules(['all']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);
				});

				it.each([
					// NOTE: custom_engine_plugin_modules is conspicuously absent from this list. That's because the
					{propName: 'config_root'},
					{propName: 'log_folder'},
					{propName: 'custom_engine_plugin_modules'}
				])('`$propName` property is present, correct, and well-annotated', async ({propName}) => {
					const goldFileName = `${propName}-section.yml`;
					const goldFileContents: string = (await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'}))
						.replaceAll('__DUMMY_CONFIG_ROOT__', PATH_TO_COMPARISON_DIR)
						.replaceAll('__DUMMY_LOG_FOLDER__', PATH_TO_PROJECT_ROOT);

					const output: string = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);

					expect(output).toContain(goldFileContents);
				});
			});

			describe('`engines` section', () => {

				let annotatedConfigModel: AnnotatedConfigModel;

				beforeAll(async () => {
					// Just select all rules for these tests, since they don't concern rule resolution.
					const ruleSelection: RuleSelection = await core.selectRules(['all']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);
				});

				it('TEMPORARY TEST: `engines` section is copied verbatim from dummy value', async () => {
					const goldFileName = 'engines-hardcoded-section.yml';
					const goldFileContents: string = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'});

					const output: string = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);

					expect(output).toContain(goldFileContents);
				});
			});

			describe('`rules` section', () => {

				let annotatedConfigModel: AnnotatedConfigModel;

				beforeAll(async () => {
					const ruleSelection: RuleSelection = await core.selectRules(['CodeStyle']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);
				});

				it.each([
					// The config overrides this rule to have the selected CodeStyle tag
					{ruleType: 'Overridden rule', ruleName: 'stub1RuleB'},
					// This rule already has the CodeStyle tag
					{ruleType: 'Non-overridden rule', ruleName: 'stub1RuleD'}
				])('Selected rules are present. Case: $ruleType', ({ruleName}) => {
					const outputString: string = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);
					// Parse the Output into a new Config and validate that it has an explicit override for the expected rule.
					const parsedOutput: CodeAnalyzerConfig = CodeAnalyzerConfig.fromYamlString(outputString);
					const tags: string[]|undefined = parsedOutput.getRuleOverrideFor('stubEngine1', ruleName).tags;
					expect(tags).toBeDefined();
					expect(tags as string[]).toContain('CodeStyle');
				});

				it.each([
					// This rule is overridden such that it LACKS the selected CodeStyle tag.
					// NOTE: Since the rule is not included in the output config, this effectively _restores_ the removed tag.
					//       This is a behavioral quirk that we are not considering a bug at this time. If enough customers
					//       complain about it, we can reassess that.
					{ruleType: 'Overridden rule', ruleName: 'stub1RuleA'},
					// This rule simply doesn't have the selected CodeStyle tag.
					{ruleType: 'Non-overridden rule', ruleName: 'stub1RuleC'},
				])('Unselected rules are absent. Case: $ruleType', async ({ruleName}) => {
					const outputString: string = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);
					// Validate that the config makes no mention of the expected rule.
					expect(outputString).not.toContain(ruleName);
					// Parse the Output into a new Config and validate that it lacks an override for the expected rule.
					const parsedOutput: CodeAnalyzerConfig = CodeAnalyzerConfig.fromYamlString(outputString);
					const override = parsedOutput.getRuleOverrideFor('stubEngine1', ruleName);
					expect(override).toEqual({});
				});

				it('EDGE CASE: When no rules are selected, `rules` section is an empty object', async () => {
					// "beep" is a nonsense tag, so there should be no rules for it.
					const ruleSelection: RuleSelection = await core.selectRules(['beep']);
					// Validate the assumption about there being no rules.
					if (ruleSelection.getCount() > 0) {
						throw new Error(`No rules should have had the tag "beep", but ${ruleSelection.getCount()} were found; test invalid`);
					}
					const model = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);

					const outputString: string = model.toFormattedOutput(OutputFormat.YAML);
					// Verify that the rules property is an empty object.
					expect(outputString).toContain(`rules: {}`);
				});

				it('EDGE CASE: When a rule with no tags is selected, its tags property is an empty array', async () => {
					// Select the tagless rule directly by its name.
					const ruleSelection: RuleSelection = await core.selectRules(['stub2RuleA']);
					// Validate the assumption about this rule being tagless.
					const tags = ruleSelection.getRule('stubEngine2', 'stub2RuleA').getTags();
					if (tags.length > 0) {
						throw new Error(`Rule stub2RuleA should be tagless, but has tags ${JSON.stringify(tags)}; test invalid`);
					}
					const model = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);

					const outputString: string = model.toFormattedOutput(OutputFormat.YAML);
					// Verify that the tags property is an empty array.
					expect(outputString).toContain(`tags: []`);
				});
			});
		});
	});
});

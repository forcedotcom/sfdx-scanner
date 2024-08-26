import path from 'node:path';
import * as fsp from 'node:fs/promises';
import {CodeAnalyzerConfig, CodeAnalyzer, RuleSelection} from '@salesforce/code-analyzer-core';

import {AnnotatedConfigModel, OutputFormat} from '../../../src/lib/models/ConfigModel';

import {FunctionalStubEnginePlugin1} from '../../stubs/StubEnginePlugins';

describe('ConfigModel implementations', () => {
	describe('AnnotatedConfigModel', () => {

		const PATH_TO_COMPARISON_DIR = path.resolve(__dirname, '..', '..', 'fixtures', 'comparison-files', 'lib', 'models', 'ConfigModel.test.ts');
		const PATH_TO_PROJECT_ROOT = path.resolve(PATH_TO_COMPARISON_DIR, '..', '..', '..', '..', '..', '..');
		const DEFAULT_CONFIG = CodeAnalyzerConfig.withDefaults();
		let inputConfig: CodeAnalyzerConfig;
		let core: CodeAnalyzer;
		let annotatedConfigModel: AnnotatedConfigModel;

		async function initializeConfigAndCore(dummyConfigRoot: string, dummyLogFolder: string): Promise<void> {
			// Read the input file as a string.
			let inputConfigString: string = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'input-config.yml'), {encoding: 'utf-8'});
			// Process it so it's usable as a config.
			inputConfigString = inputConfigString
				.replaceAll('__DUMMY_CONFIG_ROOT__', dummyConfigRoot)
				.replaceAll('__DUMMY_LOG_FOLDER__', dummyLogFolder);
			inputConfig = CodeAnalyzerConfig.fromYamlString(inputConfigString);
			core = new CodeAnalyzer(inputConfig);
			await core.addEnginePlugin(new FunctionalStubEnginePlugin1());
		}

		describe('Conversion into YAML', () => {

			describe('Properties with derived defaults', () => {

				async function verifyDefaultValues(propName: string): Promise<void> {
					const goldFileName = `${propName}-section-default.yml`;
					const goldFileContents = (await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'}))
						.replaceAll('__DUMMY_CONFIG_ROOT__', DEFAULT_CONFIG.getConfigRoot())
						.replaceAll('__DUMMY_LOG_FOLDER__', DEFAULT_CONFIG.getLogFolder());

					const output: string = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);

					expect(output).toContain(goldFileContents);
				}

				it.each([
					{propName: 'config_root'},
					{propName: 'log_folder'}
				])('When $propName is null, its output value is null and the derived value is inside a comment', async ({propName}) => {
					// Set all derivable properties to the string "null".
					await initializeConfigAndCore("null", "null");
					// These tests don't care about rule resolution, so just select all rules.
					const ruleSelection: RuleSelection = await core.selectRules(['all']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);

					await verifyDefaultValues(propName);
				});

				it.each([
					{propName: 'config_root'},
					{propName: 'log_folder'}
				])('When $propName is hardcoded to its derived default, its output value is null and the derived value is inside a comment', async ({propName}) => {
					// Set all derivable properties to the default values.
					await initializeConfigAndCore(DEFAULT_CONFIG.getConfigRoot(), DEFAULT_CONFIG.getLogFolder());
					// These tests don't care about rule resolution, so just select all rules.
					const ruleSelection: RuleSelection = await core.selectRules(['all']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);

					await verifyDefaultValues(propName);
				});

				it.each([
					{propName: 'config_root'},
					{propName: 'log_folder'}
				])('When $propName is a non-null, non-default value, that value is rendered as-is with no comment', async ({propName}) => {
					// Set all derivable properties to non-default values.
					await initializeConfigAndCore(
						// Set the config root to the comparison file's directory.
						PATH_TO_COMPARISON_DIR,
						// The log folder should be a path back up to the project root.
						PATH_TO_PROJECT_ROOT
					);

					// These tests don't care about rule resolution, so just select all rules.
					const ruleSelection: RuleSelection = await core.selectRules(['all']);
					annotatedConfigModel = AnnotatedConfigModel.fromSelection(inputConfig, ruleSelection);

					const goldFileName = `${propName}-section-overridden.yml`;
					const goldFileContents = (await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'}))
						.replaceAll('__DUMMY_CONFIG_ROOT__', PATH_TO_COMPARISON_DIR)
						.replaceAll('__DUMMY_LOG_FOLDER__', PATH_TO_PROJECT_ROOT);

					const output = annotatedConfigModel.toFormattedOutput(OutputFormat.YAML);

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

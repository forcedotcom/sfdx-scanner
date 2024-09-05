import ansis from 'ansis';
import * as EngineApi from "@salesforce/code-analyzer-engine-api";
import path from 'node:path';
import * as fsp from 'node:fs/promises';
import {CodeAnalyzerConfig, CodeAnalyzer} from '@salesforce/code-analyzer-core';

import {AnnotatedConfigModel, OutputFormat} from '../../../src/lib/models/ConfigModel';

describe('ConfigModel implementations', () => {
	describe('AnnotatedConfigModel', () => {

		const PATH_TO_COMPARISON_DIR = path.resolve(__dirname, '..', '..', 'fixtures', 'comparison-files', 'lib', 'models', 'ConfigModel.test.ts');
		const PATH_TO_PROJECT_ROOT = path.resolve(PATH_TO_COMPARISON_DIR, '..', '..', '..', '..', '..', '..');
		const DEFAULT_CONFIG = CodeAnalyzerConfig.withDefaults();
		let DEFAULT_CORE: CodeAnalyzer;
		let simulatedUserConfig: CodeAnalyzerConfig;
		let simulatedUserCore: CodeAnalyzer;
		let annotatedConfigModel: AnnotatedConfigModel;

		beforeAll(async () => {
			DEFAULT_CORE = new CodeAnalyzer(DEFAULT_CONFIG);
			await DEFAULT_CORE.addEnginePlugin(new StubEnginePlugin());
		});

		async function initializeUserConfigAndCore(dummyConfigRoot: string, dummyLogFolder: string): Promise<void> {
			// Read the input file as a string.
			let inputConfigString: string = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'input-config.yml'), {encoding: 'utf-8'});
			// Process it so it's usable as a config.
			inputConfigString = inputConfigString
				.replaceAll('__DUMMY_CONFIG_ROOT__', dummyConfigRoot)
				.replaceAll('__DUMMY_LOG_FOLDER__', dummyLogFolder);
			simulatedUserConfig = CodeAnalyzerConfig.fromYamlString(inputConfigString);
			simulatedUserCore = new CodeAnalyzer(simulatedUserConfig);
			await simulatedUserCore.addEnginePlugin(new StubEnginePlugin());
		}

		describe.each([
			// Raw YAML is raw, but the comparison files use `\n`, so we need to replace errant carriage returns.
			{yamlStyle: 'Raw YAML', format: OutputFormat.RAW_YAML, postProcessFn: ((yaml: string) => yaml.replaceAll('\r\n', '\n'))},
			// We're just testing the content, not the styling, so we need to de-style Styled YAML in addition to removing carriage returns.
			{yamlStyle: 'Styled YAML', format: OutputFormat.STYLED_YAML, postProcessFn: ((yaml: string) => ansis.strip(yaml).replaceAll('\r\n', '\n'))},
		])('Conversion into $yamlStyle', ({format, postProcessFn}) => {

			async function selectRulesAndGenerateOutput(selectors: string[], defaultSelectors: string[] = ['all']): Promise<string> {
				const simulatedUserRuleSelection = await simulatedUserCore.selectRules(selectors);
				const simulatedDefaultRuleSelection = await DEFAULT_CORE.selectRules(defaultSelectors);
				const userState = {
					config: simulatedUserConfig,
					core: simulatedUserCore,
					rules: simulatedUserRuleSelection
				};
				const defaultState = {
					config: DEFAULT_CONFIG,
					core: DEFAULT_CORE,
					rules: simulatedDefaultRuleSelection
				};
				annotatedConfigModel = AnnotatedConfigModel.fromSelection(userState, defaultState);

				return postProcessFn(annotatedConfigModel.toFormattedOutput(format));
			}

			it('Has correct header', async () => {
				// ==== SETUP ====
				// Use "null" for the dummy values, since they're unrelated to this test.
				await initializeUserConfigAndCore('null', 'null');

				// ==== TESTED BEHAVIOR ====
				// This test doesn't care about rule selection, so just select all rules.
				const output = await selectRulesAndGenerateOutput(['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'header-section.yml.goldfile'), {encoding: 'utf-8'});
				expect(output).toContain(goldFileContents);
			});

			describe.each([
				{prop: 'config_root'},
				{prop: 'log_folder'}
			])('Derivable property $prop', ({prop}) => {

				it.each([
					{scenario: 'null', inputConfig: 'null', inputLogFolder: 'null'},
					{scenario: 'the default value', inputConfig: DEFAULT_CONFIG.getConfigRoot(), inputLogFolder: DEFAULT_CONFIG.getLogFolder()},
				])('When input is $scenario, output value is null and derived default is in a comment', async ({inputConfig, inputLogFolder}) => {
					// ==== SETUP ====
					await initializeUserConfigAndCore(inputConfig, inputLogFolder);

					// ==== TESTED BEHAVIOR ====
					// This test doesn't care about rule selection, so just select all rules.
					const output: string = await selectRulesAndGenerateOutput(['all']);

					// ==== ASSERTIONS ====
					const goldFileName = `${prop}-section-default.yml.goldfile`;
					const goldFileContents = (await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'}))
						.replaceAll('__DUMMY_CONFIG_ROOT__', DEFAULT_CONFIG.getConfigRoot())
						.replaceAll('__DUMMY_LOG_FOLDER__', DEFAULT_CONFIG.getLogFolder());

					expect(output).toContain(goldFileContents);
				});

				it('When input is non-null and non-default, it is rendered as-is with no comment', async () => {
					// ==== SETUP ====
					// Set all derivable properties to non-default values.
					await initializeUserConfigAndCore(
						// Set the config root to the comparison file's directory.
						PATH_TO_COMPARISON_DIR,
						// The log folder should be a path back up to the project root.
						PATH_TO_PROJECT_ROOT
					);

					// ==== TESTED BEHAVIOR ====
					// This test doesn't care about rule selection, so just select all rules.
					const output: string = await selectRulesAndGenerateOutput(['all']);

					// ==== ASSERTIONS ====
					const goldFileName = `${prop}-section-overridden.yml.goldfile`;
					const goldFileContents = (await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, goldFileName), {encoding: 'utf-8'}))
						.replaceAll('__DUMMY_CONFIG_ROOT__', PATH_TO_COMPARISON_DIR)
						.replaceAll('__DUMMY_LOG_FOLDER__', PATH_TO_PROJECT_ROOT);

					expect(output).toContain(goldFileContents);
				});
			});

			describe('`engines` section', () => {
				let expectedEnginesHeader: string;

				beforeAll(async () => {
					expectedEnginesHeader = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'engines-section-header.yml.goldfile'), {encoding: 'utf-8'});
				});

				it.each([
					{selectedEngine: 'StubEngine1', unselectedEngine: 'StubEngine2'},
					{selectedEngine: 'StubEngine2', unselectedEngine: 'StubEngine1'}
				])('Engines corresponding to selected rules are present and correctly formatted. Engine: $selectedEngine', async ({selectedEngine, unselectedEngine}) => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Select all rules associated with an engine.
					const output = await selectRulesAndGenerateOutput([selectedEngine]);

					// ==== ASSERTIONS ====
					// Expect the header to always be there.
					expect(output).toContain(expectedEnginesHeader);

					// The output should make no mention of the engine we didn't select.
					expect(output).not.toContain(unselectedEngine);

					const goldFileContents = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, `${selectedEngine}-section.yml.goldfile`), {encoding: 'utf-8'});
					expect(output).toContain(goldFileContents);
				});

				it('Edge Case: When no rules are selected, `engines` section is an empty object with a comment', async () => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Use a selection that no rules will match.
					const output: string = await selectRulesAndGenerateOutput(['NoRuleHasThisTag']);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedEnginesHeader);
					expect(output).toContain('engines: {} # Empty object used because rule selection returned no rules');
				});
			});

			describe('`rules` section', () => {

				let expectedRulesHeader: string;

				beforeAll(async () => {
					expectedRulesHeader = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'rules-section-header.yml.goldfile'), {encoding: 'utf-8'});
				});

				it.each([
					{overrideStatus: 'overridden tags', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule1'},
					{overrideStatus: 'overridden severity', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule2'},
					{overrideStatus: 'overridden tags and severity', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule3'},
					{overrideStatus: 'no overrides', commentStatus: 'no comment', ruleName: 'Stub1Rule4'}
				])('Selected rule with $overrideStatus is present, with $commentStatus', async ({ruleName}) => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Select only the rules with "CodeStyle" as a tag.
					const output = await selectRulesAndGenerateOutput(['CodeStyle']);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedRulesHeader);

					const goldFileContents = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, `${ruleName}-section.yml.goldfile`), {encoding: 'utf-8'});
					expect(output).toContain(goldFileContents);
				});

				it.each([
					// This rule is being overridden such that it LACKS the selected CodeStyle tag.
					// NOTE: Since the rule is not included in the output config, this effectively _restores_ the removed tag.
					//       This is a behavioral quirk that we are not considering a bug at this time. If enough customers
					//       complain about it, we can reassess that.
					{ruleType: 'overridden', ruleName: 'Stub1Rule5'},
					{ruleType: 'non-overridden', ruleName: 'Stub1Rule6'},
				])('Unselected $ruleType rules are absent', async ({ruleName}) => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Select only the rules with "CodeStyle" as a tag.
					const output = await selectRulesAndGenerateOutput(['CodeStyle']);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedRulesHeader);
					// The output should make no mention of the expected rule.
					expect(output).not.toContain(ruleName);
					// If we parse the output into a new Config, then that Config should have no overrides for the expected rule.
					const parsedOutput = CodeAnalyzerConfig.fromYamlString(output);
					expect(parsedOutput.getRuleOverrideFor('StubEngine1', ruleName)).toEqual({});
				});

				it('Edge Case: When no rules are selected, `rules` section is an empty object with a comment', async () => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Use a selection that no rules will match.
					const output: string = await selectRulesAndGenerateOutput(['NoRuleHasThisTag']);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedRulesHeader);
					expect(output).toContain('rules: {} # Remove this empty object {} when you are ready to specify your first rule override');
				});

				it.each([
					{overrideStatus: 'via override', commentStatus: 'there is a comment', ruleName: 'Stub1Rule7'},
					{overrideStatus: 'by default', commentStatus: 'there is no comment', ruleName: 'Stub1Rule8'}
				])('Edge Case: When a selected rule has no tags $overrideStatus, `tags` is an empty array and $commentStatus', async ({ruleName}) => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Directly select the untagged rule by name.
					const output = await selectRulesAndGenerateOutput([ruleName]);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedRulesHeader);
					const goldFileContents = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, `${ruleName}-section.yml.goldfile`), {encoding: 'utf-8'});
					expect(output).toContain(goldFileContents);
				})

				it('Edge Case: When user-selected rules do not exist by default, no comments are applied', async () => {
					// ==== SETUP ====
					// Use "null" for the dummy values, since they're unrelated to this test.
					await initializeUserConfigAndCore('null', 'null');

					// ==== TESTED BEHAVIOR ====
					// Select the "CodeStyle" tag in both the User and Default selections.
					const output = await selectRulesAndGenerateOutput(['CodeStyle'], ['CodeStyle']);

					// ==== ASSERTIONS ====
					expect(output).toContain(expectedRulesHeader);
					const goldFileContents = await fsp.readFile(path.join(PATH_TO_COMPARISON_DIR, 'Stub1Rule3-uncommented-section.yml.goldfile'), {encoding: 'utf-8'});
					expect(output).toContain(goldFileContents);
				});
			});
		});
	});
});

// ====== STUBS ======

class StubEnginePlugin extends EngineApi.EnginePluginV1 {

	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	private readonly descriptionsByEngine: {[key: string]: EngineApi.ConfigDescription} = {
		StubEngine1: {
			overview: 'This is a generic overview for StubEngine1\nIt has multiple lines of text\nWhee!',
			fieldDescriptions: {
				'Property1': 'This is the description for Property1',
				// Property2 is undocumented
				'Property3': 'This is the description for Property3',
				'Property4': 'This is the description for Property4',
				'Property5': 'This is the description for Property5',
				'Property6': 'This is the description for Property6'
			}
		}
		// StubEngine2 has no overview and no documented properties.
	}

	public getAvailableEngineNames(): string[] {
		return ['StubEngine1', 'StubEngine2'];
	}

	public createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName === 'StubEngine1') {
			this.createdEngines.set(engineName, new StubEngine1(config));
		} else if (engineName === 'StubEngine2') {
			this.createdEngines.set(engineName, new StubEngine2(config));
		} else {
			throw new Error(`No engine named ${engineName}`);
		}
		return Promise.resolve(this.getCreatedEngine(engineName));
	}

	public describeEngineConfig(engineName: string): EngineApi.ConfigDescription {
		return this.descriptionsByEngine[engineName] ?? {};
	}

	public getCreatedEngine(engineName: string): EngineApi.Engine {
		if (this.createdEngines.has(engineName)) {
			return this.createdEngines.get(engineName) as EngineApi.Engine;
		}
		throw new Error(`Engine named ${engineName} not yet instantiated`);
	}
}

class StubEngine1 extends EngineApi.Engine {

	public constructor(_config: EngineApi.ConfigObject) {
		super();
	}

	public getName(): string {
		return 'StubEngine1';
	}

	public describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([{
			name: 'Stub1Rule1',
			severityLevel: EngineApi.SeverityLevel.Info,
			type: EngineApi.RuleType.Standard,
			tags: ["Recommended", "CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule2',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			type: EngineApi.RuleType.Standard,
			tags: ["CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule3',
			severityLevel: EngineApi.SeverityLevel.Low,
			type: EngineApi.RuleType.Standard,
			tags: ["BestPractices"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule4',
			severityLevel: EngineApi.SeverityLevel.High,
			type: EngineApi.RuleType.Standard,
			tags: ["CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule5',
			severityLevel: EngineApi.SeverityLevel.High,
			type: EngineApi.RuleType.Standard,
			tags: ["Recommended", "CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule6',
			severityLevel: EngineApi.SeverityLevel.Low,
			type: EngineApi.RuleType.Standard,
			tags: ["Recommended"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule7',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			type: EngineApi.RuleType.Standard,
			tags: [],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule8',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			type: EngineApi.RuleType.Standard,
			tags: ['Recommended'],
			description: 'Generic description',
			resourceUrls: []
		}]);
	}

	public runRules(): Promise<EngineApi.EngineRunResults> {
		return Promise.resolve({
			violations: []
		});
	}
}

class StubEngine2 extends EngineApi.Engine {
	public constructor(_config: EngineApi.ConfigObject) {
		super();
	}

	public getName(): string {
		return 'StubEngine2';
	}

	public describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([{
			name: 'Stub2Rule1',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			type: EngineApi.RuleType.Standard,
			tags: ['Security'],
			description: 'Generic description',
			resourceUrls: []
		}]);
	}

	public runRules(): Promise<EngineApi.EngineRunResults> {
		return Promise.resolve({
			violations: []
		});
	}

}

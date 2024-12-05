import path from 'node:path';
import * as fs from 'node:fs';
import * as fsp from 'node:fs/promises';
import ansis from 'ansis';
import {CodeAnalyzerConfig} from "@salesforce/code-analyzer-core";
import * as EngineApi from "@salesforce/code-analyzer-engine-api";

import {CodeAnalyzerConfigFactory} from "../../../src/lib/factories/CodeAnalyzerConfigFactory";
import {EnginePluginsFactory} from '../../../src/lib/factories/EnginePluginsFactory';
import {ConfigAction, ConfigDependencies, ConfigInput} from '../../../src/lib/actions/ConfigAction';
import {ConfigStyledYamlViewer} from '../../../src/lib/viewers/ConfigViewer';
import {ConfigActionSummaryViewer} from '../../../src/lib/viewers/ActionSummaryViewer';

import {SpyConfigWriter} from '../../stubs/SpyConfigWriter';
import {SpyConfigViewer} from '../../stubs/SpyConfigViewer';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';

const PATH_TO_FIXTURES = path.join(__dirname, '..', '..', 'fixtures');

const PATH_TO_EXAMPLE_WORKSPACE = path.join(PATH_TO_FIXTURES, 'example-workspaces', 'ConfigAction.test.ts');

describe('ConfigAction tests', () => {
	const PATH_TO_COMPARISON_DIR = path.join(PATH_TO_FIXTURES, 'comparison-files', 'lib', 'actions', 'ConfigAction.test.ts');

	let spyDisplay: SpyDisplay;
	let dependencies: ConfigDependencies;

	describe('Config Resolution', () => {

		describe('When there IS NOT an existing config...', () => {

			beforeEach(() => {
				spyDisplay = new SpyDisplay();
				dependencies = {
					logEventListeners: [],
					progressEventListeners: [],
					viewer: new ConfigStyledYamlViewer(spyDisplay),
					configFactory: new DefaultStubCodeAnalyzerConfigFactory(),
					actionSummaryViewer: new ConfigActionSummaryViewer(spyDisplay),
					pluginsFactory: new StubEnginePluginFactory()
				};
			});

			it.each([
				{position: 'start'},
				{position: 'end'}
			])('Top-level $position comment is correct', async ({position}) => {
				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'header-comments', `top-level-${position}.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{section: 'config_root'},
				{section: 'log_folder'},
				{section: 'rules'},
				{section: 'engines'}
			])('`$section` section header-comment is correct', async ({section}) => {
				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'header-comments', `${section}-section.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{prop: 'config_root'},
				{prop: 'log_folder'}
			])('Derivable property $prop is null, and derived value is in a comment', async ({prop}) => {
				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const defaultConfig = CodeAnalyzerConfig.withDefaults();
				const goldFileContents = (await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'derivables-as-defaults', `${prop}.yml.goldfile`)))
					.replaceAll('__DUMMY_CONFIG_ROOT__', JSON.stringify(defaultConfig.getConfigRoot()))
					.replaceAll('__DUMMY_LOG_FOLDER__', JSON.stringify(defaultConfig.getLogFolder()));
				expect(output).toContain(goldFileContents);
			});

			it('Selected rules are present and uncommented', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select the rules with the CodeStyle tag
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				// Rather than exhaustively check every rule, we'll just check one, because if that one is correct then
				// we can reasonably assume that the other rules are also present and correct.
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'default-configurations', 'Stub1Rule1.yml.goldfile'));
				expect(output).toContain(goldFileContents);
			});

			it('Unselected rules are absent', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select the rules with the CodeStyle tag
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				// Rather than exhaustively check every rule, we'll just check one, because if that rule is properly
				// absent then we can reasonably assume that the other ones are too.
				expect(output).not.toContain('Stub1Rule3');
			});

			it('Engines for selected rules use their default configuration', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select the rules with the CodeStyle tag
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				// Only one engine has rules that got returned.
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'default-configurations', 'StubEngine1.yml.goldfile'));
				expect(output).toContain(goldFileContents);
			});

			it('Engines for unselected rules have no configuration', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select the rules with the CodeStyle tag
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				// StubEngine2 has no rules with the CodeStyle tag.
				expect(output).not.toContain('StubEngine2');
			});

			it('Edge case: When no rules are selected, `rules` and `engines` sections are commented empty objects', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select the tag "NoRuleHasThisTag"
				const output = await runActionAndGetDisplayedConfig(dependencies, ['NoRuleHasThisTag']);

				// ==== ASSERTIONS ====
				expect(output).toContain('rules: {} # Remove this empty object {} when you are ready to specify your first rule override');
				expect(output).toContain('engines: {} # Empty object used because rule selection returned no rules');
			});

			it('Edge case: When a selected rule has no tags by default, `tags` is an empty array with no comment', async () => {
				// ==== TESTED BEHAVIOR ====
				// Select Stub1Rule7, which has no tags, by its name directly.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['Stub1Rule7']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'default-configurations', 'Stub1Rule7.yml.goldfile'));
				expect(output).toContain(goldFileContents);
			});
		});

		describe('When there IS an existing config...', () => {

			let stubConfigFactory: AlternativeStubCodeAnalyzerConfigFactory;

			beforeEach(() => {
				stubConfigFactory = new AlternativeStubCodeAnalyzerConfigFactory();
				spyDisplay = new SpyDisplay();
				dependencies = {
					logEventListeners: [],
					progressEventListeners: [],
					viewer: new ConfigStyledYamlViewer(spyDisplay),
					configFactory: stubConfigFactory,
					actionSummaryViewer: new ConfigActionSummaryViewer(spyDisplay),
					pluginsFactory: new StubEnginePluginFactory()
				};
			});


			it.each([
				{position: 'start'},
				{position: 'end'}
			])('Top-level $position comment is correct', async ({position}) => {
				// ==== SETUP ====
				// Set the dummy config properties to null; it's fine for this test.
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'header-comments', `top-level-${position}.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{section: 'config_root'},
				{section: 'log_folder'},
				{section: 'rules'},
				{section: 'engines'}
			])('`$section` section header-comment is correct', async ({section}) => {
				// ==== SETUP ====
				// Set the dummy config properties to null; it's fine for this test.
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'header-comments', `${section}-section.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{prop: 'config_root'},
				{prop: 'log_folder'}
			])('If derivable property $prop is explicitly null, then output is null and derived value is in a comment', async ({prop}) => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const defaultConfig = CodeAnalyzerConfig.withDefaults();
				const goldFileContents = (await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'derivables-as-defaults', `${prop}.yml.goldfile`)))
					.replaceAll('__DUMMY_CONFIG_ROOT__', JSON.stringify(defaultConfig.getConfigRoot()))
					.replaceAll('__DUMMY_LOG_FOLDER__', JSON.stringify(defaultConfig.getLogFolder()));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{prop: 'config_root'},
				{prop: 'log_folder'}
			])('If derivable property $prop is explicitly its default value, then output is null and derived value is in a comment', async ({prop}) => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot(CodeAnalyzerConfig.withDefaults().getConfigRoot());
				stubConfigFactory.setDummyLogFolder(CodeAnalyzerConfig.withDefaults().getLogFolder());

				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const defaultConfig = CodeAnalyzerConfig.withDefaults();
				const goldFileContents = (await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'derivables-as-defaults', `${prop}.yml.goldfile`)))
					.replaceAll('__DUMMY_CONFIG_ROOT__', JSON.stringify(defaultConfig.getConfigRoot()))
					.replaceAll('__DUMMY_LOG_FOLDER__', JSON.stringify(defaultConfig.getLogFolder()));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{prop: 'config_root'},
				{prop: 'log_folder'}
			])(`When derivable property $prop input is non-null and non-default, it is rendered as-is`, async ({prop}) => {
				// ==== SETUP ====
				// Make the config root and log folder both be the folder above this one.
				const parentOfCurrentDirectory = path.resolve(__dirname, '..');
				stubConfigFactory.setDummyConfigRoot(parentOfCurrentDirectory);
				stubConfigFactory.setDummyLogFolder(parentOfCurrentDirectory);

				// ==== TESTED BEHAVIOR ====
				// Just select all rules for this test, since we don't care about the rules here.
				const output = await runActionAndGetDisplayedConfig(dependencies, ['all']);

				// ==== ASSERTIONS ====
				const goldFileContents = (await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'derivables-as-non-defaults', `${prop}.yml.goldfile`)))
					.replace('__DUMMY_CONFIG_ROOT__', parentOfCurrentDirectory)
					.replace('__DUMMY_LOG_FOLDER__', parentOfCurrentDirectory)
					.replace('__DUMMY_DEFAULT_CONFIG_ROOT__', JSON.stringify(null))
					.replace('__DUMMY_DEFAULT_LOG_FOLDER__', JSON.stringify(null))
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{overrideStatus: 'overridden tags', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule1'},
				{overrideStatus: 'overridden severity', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule2'},
				{overrideStatus: 'overridden tags and severity', commentStatus: 'comment indicating original values', ruleName: 'Stub1Rule3'},
				{overrideStatus: 'no overrides', commentStatus: 'no comment', ruleName: 'Stub1Rule4'},
			])('Selected and enabled rules with $overrideStatus are present with $commentStatus', async ({ruleName}) => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'override-configurations', `${ruleName}.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it.each([
				{ruleType: 'Overridden and unselected rules', ruleName: 'Stub1Rule5'},
				{ruleType: 'Non-overridden and unselected rules', ruleName: 'Stub1Rule6'},
				{ruleType: 'Selected rules on disabled engines', ruleName: 'Stub3Rule1'},
			])('$ruleType are absent', async ({ruleName}) => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				expect(output).not.toContain(ruleName);
			});

			it('Engines for selected rules use their existing configuration', async () => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'override-configurations', `StubEngine1.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it('Engines for unselected rules have no configuration', async () => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				expect(output).not.toContain('StubEngine2');
			});

			it('Disabled engines with selected rules use their configuration', async () => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['CodeStyle']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'override-configurations', `StubEngine3.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it('Edge Case: When no rules are selected, `rules` and `engines` sections are commented empty objects', async () => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				// Select the tag "NoRuleHasThisTag"
				const output = await runActionAndGetDisplayedConfig(dependencies, ['NoRuleHasThisTag']);

				// ==== ASSERTIONS ====
				expect(output).toContain('rules: {} # Remove this empty object {} when you are ready to specify your first rule override');
				expect(output).toContain('engines: {} # Empty object used because rule selection returned no rules');
			});

			it.each([
				{overrideStatus: 'via override', commentStatus: 'there is a comment', ruleName: 'Stub1Rule7'},
				{overrideStatus: 'by default', commentStatus: 'there is no comment', ruleName: 'Stub1Rule8'}
			])('Edge Case: When selected rule has no tags $overrideStatus, `tags` is an empty array and $commentStatus', async ({ruleName}) => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot('null');
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, [ruleName]);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'override-configurations', `${ruleName}.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});

			it('If config is provided with relative path to config_root, then it remains relative in config output even though core makes it absolute for engines', async () => {
				// ==== SETUP ====
				stubConfigFactory.setDummyConfigRoot(PATH_TO_EXAMPLE_WORKSPACE);
				stubConfigFactory.setDummyLogFolder('null');

				// ==== TESTED BEHAVIOR ====
				const output = await runActionAndGetDisplayedConfig(dependencies, ['Stub2Rule1']);

				// ==== ASSERTIONS ====
				const goldFileContents = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'override-configurations', `StubEngine2_forConfigWithRelativePathScenario.yml.goldfile`));
				expect(output).toContain(goldFileContents);
			});
		});
	});

	describe('File Creation', () => {
		beforeEach(() => {
			spyDisplay = new SpyDisplay();
			dependencies = {
				logEventListeners: [],
				progressEventListeners: [],
				viewer: new ConfigStyledYamlViewer(spyDisplay),
				configFactory: new DefaultStubCodeAnalyzerConfigFactory(),
				actionSummaryViewer: new ConfigActionSummaryViewer(spyDisplay),
				pluginsFactory: new StubEnginePluginFactory()
			};
		});

		it('If a file is created, then the ConfigViewer is unused', async () => {
			// ==== SETUP ====
			// We need to add a Writer to the dependencies.
			const spyWriter = new SpyConfigWriter();
			dependencies.writer = spyWriter;
			// Replace the viewer with a Spy.
			const spyViewer = new SpyConfigViewer();
			dependencies.viewer = spyViewer;

			// ==== TESTED BEHAVIOR ====
			await runActionAndGetDisplayedConfig(dependencies, ['all']);

			// ==== ASSERTIONS ====
			expect(spyWriter.getCallHistory()).toHaveLength(1);
			expect(spyViewer.getCallHistory()).toHaveLength(0);
		});

		it('If a file is specified by not created, then the ConfigViewer is used', async () => {
			// ==== SETUP ====
			// We need to add a Writer to the dependencies.
			const spyWriter = new SpyConfigWriter(false);
			dependencies.writer = spyWriter;
			// Replace the viewer with a Spy.
			const spyViewer = new SpyConfigViewer();
			dependencies.viewer = spyViewer;

			// ==== TESTED BEHAVIOR ====
			await runActionAndGetDisplayedConfig(dependencies, ['all']);

			// ==== ASSERTIONS ====
			expect(spyWriter.getCallHistory()).toHaveLength(1);
			expect(spyViewer.getCallHistory()).toHaveLength(1);
		});
	});

	describe('Summary generation', () => {
		beforeEach(() => {
			spyDisplay = new SpyDisplay();
			dependencies = {
				logEventListeners: [],
				progressEventListeners: [],
				viewer: new ConfigStyledYamlViewer(spyDisplay),
				configFactory: new DefaultStubCodeAnalyzerConfigFactory(),
				actionSummaryViewer: new ConfigActionSummaryViewer(spyDisplay),
				pluginsFactory: new StubEnginePluginFactory()
			}
		});

		it('When an Outfile is created, it is mentioned by the Summarizer', async () => {
			// ==== SETUP ====
			// Assign a Writer to the dependencies.
			dependencies.writer = new SpyConfigWriter(true);

			// ==== TESTED BEHAVIOR ====
			// Invoke the action, specifying an outfile.
			const action = ConfigAction.createAction(dependencies);
			const input: ConfigInput = {
				'rule-selector': ['all'],
				'output-file': 'out-config.yml'
			};
			await action.execute(input);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			const displayedLogEvents = ansis.strip(displayEvents
				.filter(e => e.type === DisplayEventType.LOG)
				.map(e => e.data)
				.join('\n'));

			const goldfileContents: string = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'action-summaries', 'outfile-created.txt.goldfile'));
			expect(displayedLogEvents).toContain(goldfileContents);
		});

		it.each([
			{case: 'an Outfile is specified but not written', writer: new SpyConfigWriter(false), outfile: 'out-config.yml'},
			{case: 'an Outfile is not specified at all', writer: undefined, outfile: undefined}
		])('When $case, the Summarizer mentions no outfile', async ({writer, outfile}) => {
			// ==== SETUP ====
			// Add the specified Writer (or lack-of-Writer) to the dependencies.
			dependencies.writer = writer;

			// ==== TESTED BEHAVIOR ====
			// Invoke the action, specifying an outfile (or lack of one).
			const action = ConfigAction.createAction(dependencies);
			const input: ConfigInput = {
				'rule-selector': ['all'],
				'output-file': outfile
			};
			await action.execute(input);

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			const displayedLogEvents = ansis.strip(displayEvents
				.filter(e => e.type === DisplayEventType.LOG)
				.map(e => e.data)
				.join('\n'));

			const goldfileContents: string = await readGoldFile(path.join(PATH_TO_COMPARISON_DIR, 'action-summaries', 'no-outfile-created.txt.goldfile'));
			expect(displayedLogEvents).toContain(goldfileContents);
		});
	})
	// ====== HELPER FUNCTIONS ======

	async function readGoldFile(goldFilePath: string): Promise<string> {
		return fsp.readFile(goldFilePath, {encoding: 'utf-8'});
	}

	async function runActionAndGetDisplayedConfig(dependencies: ConfigDependencies, ruleSelectors: string[], configFile?: string): Promise<string> {
		// ==== SETUP ====
		const action = ConfigAction.createAction(dependencies);
		const input: ConfigInput = {
			'rule-selector': ruleSelectors,
			'config-file': configFile
		};

		// ==== TESTED BEHAVIOR ====
		await action.execute(input);

		// ==== OUTPUT PROCESSING ====
		const displayEvents = spyDisplay.getDisplayEvents();
		expect(displayEvents[0].type).toEqual(DisplayEventType.LOG);
		return ansis.strip(displayEvents[0].data);
	}
});

// ====== STUBS ======

class DefaultStubCodeAnalyzerConfigFactory implements CodeAnalyzerConfigFactory {
	public create(): CodeAnalyzerConfig {
		return CodeAnalyzerConfig.withDefaults();
	}
}

class AlternativeStubCodeAnalyzerConfigFactory implements CodeAnalyzerConfigFactory {
	private dummyConfigRoot: string;
	private dummyLogFolder: string;

	public setDummyConfigRoot(dummyConfigRoot: string): void {
		this.dummyConfigRoot = dummyConfigRoot;
	}

	public setDummyLogFolder(dummyLogFolder: string): void {
		this.dummyLogFolder = dummyLogFolder;
	}

	public create(): CodeAnalyzerConfig {
		const rawConfigFileContents = fs.readFileSync(path.join(PATH_TO_EXAMPLE_WORKSPACE, 'optional-input-config.yml'), 'utf-8');
		const validatedConfigFileContents = rawConfigFileContents
			.replaceAll('__DUMMY_CONFIG_ROOT__', this.dummyConfigRoot)
			.replaceAll('__DUMMY_LOG_FOLDER__', this.dummyLogFolder)
			.replaceAll('__DUMMY_STUBENGINE2_SUBFIELD__', this.dummyConfigRoot && this.dummyConfigRoot !== 'null' ?
				path.join(this.dummyConfigRoot, 'optional-input-config.yml') : 'dummy');
		return CodeAnalyzerConfig.fromYamlString(validatedConfigFileContents,  process.cwd());
	}
}

class StubEnginePluginFactory implements EnginePluginsFactory {
	public create(): EngineApi.EnginePlugin[] {
		return [
			new StubEnginePlugin()
		];
	}
}

class StubEnginePlugin extends EngineApi.EnginePluginV1 {

	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	/*
	    descriptionText: string;
    valueType: string;
    defaultValue: ConfigValue;
	 */

	private readonly descriptionsByEngine: {[key: string]: EngineApi.ConfigDescription} = {
		StubEngine1: {
			overview: 'This is a generic overview for StubEngine1\nIt has multiple lines of text\nWhee!',
			fieldDescriptions: {
				'Property1': {
					descriptionText: 'This is the description for Property1',
					valueType: 'string',
					defaultValue: 'default1'
				},
				// Property2 is undocumented
				'Property3': {
					descriptionText: 'This is the description for Property3',
					valueType: 'string',
					defaultValue: 'default3'
				},
				'Property4': {
					descriptionText: 'This is the description for Property4',
					valueType: 'object',
					defaultValue: {SubProperty1: 10, SubProperty2: true}
				},
				'Property5': {
					descriptionText: 'This is the description for Property5',
					valueType: 'array',
					defaultValue: ['arr1', 'arr2']
				},
				'Property6': {
					descriptionText: 'This is the description for Property6',
					valueType: 'string',
					defaultValue: null
				},
			}
		},
		StubEngine2: {
			overview: 'Some overview for StubEngine2',
			fieldDescriptions: {
				'top_field': {
					descriptionText: 'Some description for top_field',
					valueType: 'object',
					defaultValue: {}
				}
			}
		}
		// StubEngine3 also has no overview or documented properties.
	}

	public getAvailableEngineNames(): string[] {
		return ['StubEngine1', 'StubEngine2', 'StubEngine3'];
	}

	public createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName === 'StubEngine1') {
			this.createdEngines.set(engineName, new StubEngine1(config));
		} else if (engineName === 'StubEngine2') {
			this.createdEngines.set(engineName, new StubEngine2(config));
		} else if (engineName === 'StubEngine3') {
			this.createdEngines.set(engineName, new StubEngine3(config));
		} else {
			throw new Error(`No engine named ${engineName}`);
		}
		return Promise.resolve(this.getCreatedEngine(engineName));
	}

	public createEngineConfig(engineName: string, configValueExtractor: EngineApi.ConfigValueExtractor): Promise<EngineApi.ConfigObject> {
		if (engineName === 'StubEngine1') {
			return Promise.resolve({
				Property1: configValueExtractor.extractString('Property1', 'default1')!,
				Property2: configValueExtractor.extractString('Property2', 'default2')!,
				Property3: configValueExtractor.extractString('Property3', 'default3')!,
				Property4: configValueExtractor.extractObject('Property4', {SubProperty1: 10, SubProperty2: true})!,
				Property5: configValueExtractor.extractArray('Property5', EngineApi.ValueValidator.validateString, ['arr1', 'arr2'])!
			});
		} else if (engineName === 'StubEngine2') {
			return Promise.resolve(configValueExtractor.getObject());
		} else if (engineName === 'StubEngine3') {
			return Promise.resolve(configValueExtractor.getObject());
		} else {
			throw new Error('Cannot configure unknown engine ' + engineName);
		}
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

	getEngineVersion(): Promise<string> {
		return Promise.resolve("1.0.0");
	}

	public describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([{
			name: 'Stub1Rule1',
			severityLevel: EngineApi.SeverityLevel.Info,
			tags: ["Recommended", "CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule2',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			tags: ["CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule3',
			severityLevel: EngineApi.SeverityLevel.Low,
			tags: ["BestPractices"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule4',
			severityLevel: EngineApi.SeverityLevel.High,
			tags: ["CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule5',
			severityLevel: EngineApi.SeverityLevel.High,
			tags: ["Recommended", "CodeStyle"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule6',
			severityLevel: EngineApi.SeverityLevel.Low,
			tags: ["Recommended"],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule7',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			tags: [],
			description: 'Generic description',
			resourceUrls: []
		}, {
			name: 'Stub1Rule8',
			severityLevel: EngineApi.SeverityLevel.Moderate,
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

	getEngineVersion(): Promise<string> {
		return Promise.resolve("1.0.2");
	}

	public describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([{
			name: 'Stub2Rule1',
			severityLevel: EngineApi.SeverityLevel.Moderate,
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

class StubEngine3 extends EngineApi.Engine {
	public constructor(_config: EngineApi.ConfigObject) {
		super();
	}

	public getName(): string {
		return 'StubEngine3';
	}

	getEngineVersion(): Promise<string> {
		return Promise.resolve("1.0.3");
	}

	public describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([{
			name: 'Stub3Rule1',
			severityLevel: EngineApi.SeverityLevel.Moderate,
			tags: ['CodeStyle'],
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

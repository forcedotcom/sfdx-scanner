import {CodeAnalyzer, CodeAnalyzerConfig, LogLevel} from '@salesforce/code-analyzer-core';
import {LogEventDisplayer} from '../../../src/lib/listeners/LogEventListener';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import {EventConfigurableEngine1, ConfigurableStubEnginePlugin1} from "../../stubs/StubEnginePlugins";

describe('LogEventListener implementations', () => {
	let spyDisplay: SpyDisplay;
	let engine1: EventConfigurableEngine1;
	let stubEnginePlugin: ConfigurableStubEnginePlugin1;
	let core: CodeAnalyzer;

	beforeEach(async () => {
		spyDisplay = new SpyDisplay();
		engine1 = new EventConfigurableEngine1({});
		stubEnginePlugin = new ConfigurableStubEnginePlugin1();
		stubEnginePlugin.addEngine(engine1);
		core = new CodeAnalyzer(CodeAnalyzerConfig.withDefaults());
		await core.addEnginePlugin(stubEnginePlugin);
	});

	describe('LogEventDisplayer', () => {
		let logEventDisplayer: LogEventDisplayer;

		beforeEach(() => {
			logEventDisplayer = new LogEventDisplayer(spyDisplay);
		});

		it.each([
			{levelName: "Error", logLevel: LogLevel.Error, displayType: 'warnings', displayEvent: DisplayEventType.WARN},
			{levelName: "Warn", logLevel: LogLevel.Warn, displayType: 'warnings', displayEvent: DisplayEventType.WARN},
			{levelName: "Info", logLevel: LogLevel.Info, displayType: 'info-logs', displayEvent: DisplayEventType.INFO},
		])('Displays $levelName-type log events as $displayType', async ({logLevel, displayEvent}) => {
			// ==== TEST SETUP ====
			const expectedMessages: string[] = ['message1', 'message2', 'message3'];
			for (const expectedMessage of expectedMessages) {
				engine1.addEvents({logLevel, message: expectedMessage});
			}
			const ruleSelection = core.selectRules('all');

			// ==== TESTED BEHAVIOR ====
			logEventDisplayer.listen(core);
			await core.run(ruleSelection, {
				// This does not matter.
				workspaceFiles: ['package.json']
			});

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(3);
			for (let i = 0; i < 3; i++) {
				expect(displayEvents[i].type).toEqual(displayEvent);
				expect(displayEvents[i].data).toContain(expectedMessages[i]);
			}
		});

		it.each([
			{levelName: "Debug", logLevel: LogLevel.Debug},
			{LevelName: 'Fine', logLevel: LogLevel.Fine}
		])('Does NOT display events of type $levelName', async ({logLevel}) => {
			// ==== TEST SETUP ====
			const messages = ['message1', 'message2', 'message3'];
			for (const message of messages) {
				engine1.addEvents({logLevel, message});
			}
			const ruleSelection = core.selectRules('all');

			// ==== TESTED BEHAVIOR ====
			logEventDisplayer.listen(core);
			await core.run(ruleSelection, {
				// This does not matter.
				workspaceFiles: ['package.json']
			});

			// ==== ASSERIONTS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(0);
		});
	});
})

import {CodeAnalyzer, CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {LogLevel} from '@salesforce/code-analyzer-engine-api';
import {LogEventDisplayer} from '../../../src/lib/listeners/LogEventListener';
import {DisplayEventType, SpyDisplay} from '../../stubs/SpyDisplay';
import {SpyLogWriter} from '../../stubs/SpyLogWriter';
import {EventConfigurableEngine1, ConfigurableStubEnginePlugin1} from "../../stubs/StubEnginePlugins";
import {LogEventLogger} from "../../../src/lib/listeners/LogEventListener";

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
			{levelName: "Error", logLevel: LogLevel.Error, displayType: 'errors', displayEvent: DisplayEventType.ERROR},
			{levelName: "Warn", logLevel: LogLevel.Warn, displayType: 'warnings', displayEvent: DisplayEventType.WARN},
			{levelName: "Info", logLevel: LogLevel.Info, displayType: 'info-logs', displayEvent: DisplayEventType.INFO},
		])('Displays $levelName-type log events as $displayType', async ({logLevel, displayEvent}) => {
			// ==== TEST SETUP ====
			const expectedMessages: string[] = ['message1', 'message2', 'message3'];
			for (const expectedMessage of expectedMessages) {
				engine1.addEvents({logLevel, message: expectedMessage});
			}
			// The specific files we target in our workspace don't matter.
			const workspace = await core.createWorkspace(['package.json']);
			const ruleSelection = await core.selectRules(['all'], {workspace});

			// ==== TESTED BEHAVIOR ====
			logEventDisplayer.listen(core);
			await core.run(ruleSelection, {
				workspace
			});
			logEventDisplayer.stopListening();

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
			{levelName: 'Fine', logLevel: LogLevel.Fine}
		])('Does NOT display events of type $levelName', async ({logLevel}) => {
			// ==== TEST SETUP ====
			const messages = ['message1', 'message2', 'message3'];
			for (const message of messages) {
				engine1.addEvents({logLevel, message});
			}
			// The specific files we include in our workspace don't matter.
			const workspace = await core.createWorkspace(['package.json']);
			const ruleSelection = await core.selectRules(['all'], {workspace});

			// ==== TESTED BEHAVIOR ====
			logEventDisplayer.listen(core);
			await core.run(ruleSelection, {workspace});
			logEventDisplayer.stopListening();

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(0);
		});
	});

	describe('LogEventLogger', () => {

		it.each([
			{levelName: 'Error', logLevel: LogLevel.Error},
			{levelName: 'Warn', logLevel: LogLevel.Warn},
			{levelName: 'Info', logLevel: LogLevel.Info},
			{levelName: 'Debug', logLevel: LogLevel.Debug},
			{levelName: 'Fine', logLevel: LogLevel.Fine}
		])(`Writes to logfile for events of level $levelName`, async ({levelName, logLevel}) => {
			// ==== TEST SETUP ====
			const expectedMessages = ['message1', 'message2', 'message3'];
			for (const expectedMessage of expectedMessages) {
				engine1.addEvents({logLevel, message: expectedMessage});
			}
			// The specific files we include in our workspace don't matter.
			const workspace = await core.createWorkspace(['package.json']);
			const ruleSelection = await core.selectRules(['all'], {workspace});

			const spyLogWriter = new SpyLogWriter();
			const logListener = new LogEventLogger(spyLogWriter);

			// ==== TESTED BEHAVIOR ====
			logListener.listen(core);
			await core.run(ruleSelection, {workspace});
			logListener.stopListening();

			// ==== ASSERTIONS ====
			// Verify that the right messages were logged.
			const logContents = spyLogWriter.getWrittenLog();
			for (const expectedMessage of expectedMessages) {
				expect(logContents).toMatch(new RegExp(`${levelName}.+${expectedMessage}`));
			}
		});
	});
})

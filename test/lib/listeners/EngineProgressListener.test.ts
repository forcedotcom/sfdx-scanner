import {CodeAnalyzer, CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {SpinnerProgressListener} from '../../../src/lib/listeners/EngineProgressListener';
import {SpyDisplay, DisplayEvent, DisplayEventType} from '../../stubs/SpyDisplay';
import {TimeableStubEnginePlugin1, TimeableEngine1} from '../../stubs/StubEnginePlugins';
import {StubEnginePluginsFactory_withFunctionalStubEngine} from '../../stubs/StubEnginePluginsFactories';

describe('EngineProgressListener implementations', () => {
	describe('SpinnerProgressListener', () => {
		let codeAnalyzer: CodeAnalyzer;
		let spyDisplay: SpyDisplay;
		let listener: SpinnerProgressListener;

		beforeEach(() => {
			codeAnalyzer = new CodeAnalyzer(CodeAnalyzerConfig.withDefaults());
			spyDisplay = new SpyDisplay();
		});

		afterEach(() => {
			listener.stopListening();
		});

		it('Listens to updates for each selected engines', async () => {
			// ==== TEST SETUP ====
			//  Assign our standard functional stubs to the core.
			const factory = new StubEnginePluginsFactory_withFunctionalStubEngine();
			for (const enginePlugin of factory.create()) {
				await codeAnalyzer.addEnginePlugin(enginePlugin);
			}
			// Select all rules.
			const ruleSelection = await codeAnalyzer.selectRules(['all']);
			// We don't want automated ticking to mess with the messages, so just turn it off for now.
			listener = new SpinnerProgressListener(spyDisplay, -1);

			// ==== TESTED BEHAVIOR ====
			// Start listening, then immediately stop. Since we're not executing the engines, there's nothing to listen
			// _for_, but that's fine because this test is just about whether we _start_ listening.
			listener.listen(codeAnalyzer, ruleSelection);
			listener.stopListening();

			// ==== ASSERTIONS ====
			// Expect the initialization event and the ending event.
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			const startEvent = displayEvents[0];
			expect(startEvent).toHaveProperty('type', DisplayEventType.SPINNER_START);
			// Since we selected all engines, we expect all engines to have completion status portions.
			expect(startEvent.data).toContain('- stubEngine1 at 0% completion');
			expect(startEvent.data).toContain('- stubEngine2 at 0% completion');
			const endEvent = displayEvents[1];
			expect(endEvent).toHaveProperty('type', DisplayEventType.SPINNER_STOP);
			expect(endEvent.data).toContain('done');
		});

		it('Does not listen for non-selected engines', async () => {
			// ==== TEST SETUP ====
			//  Assign our standard functional stubs to the core.
			const factory = new StubEnginePluginsFactory_withFunctionalStubEngine();
			for (const enginePlugin of factory.create()) {
				await codeAnalyzer.addEnginePlugin(enginePlugin);
			}
			// Select the rules from only one engine.
			const ruleSelection = await codeAnalyzer.selectRules(['stubEngine1']);
			// We don't want automated ticking to mess with the messages, so just turn it off for now.
			listener = new SpinnerProgressListener(spyDisplay, -1);

			// ==== TESTED BEHAVIOR ====
			// Start listening, then immediately stop. Since we're not executing the engines, there's nothing to listen
			// _for_, but that's fine because this test is just about whether we _start_ listening.
			listener.listen(codeAnalyzer, ruleSelection);
			listener.stopListening();

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			expect(displayEvents).toHaveLength(2);
			const startEvent = displayEvents[0];
			expect(startEvent).toHaveProperty('type', DisplayEventType.SPINNER_START);
			// The engine whose rules we selected should have a completion status.
			expect(startEvent.data).toContain('- stubEngine1 at 0% completion');
			// The engine we didn't select should not.
			expect(startEvent.data).not.toContain('stubEngine2');
			const endEvent = displayEvents[1];
			expect(endEvent).toHaveProperty('type', DisplayEventType.SPINNER_STOP);
			expect(endEvent.data).toContain('done');
		});

		it('Properly processes progress updates from engine', async () => {
			// ==== TEST SETUP ====
			//  Assign our standard functional stubs to the core.
			const factory = new StubEnginePluginsFactory_withFunctionalStubEngine();
			for (const enginePlugin of factory.create()) {
				await codeAnalyzer.addEnginePlugin(enginePlugin);
			}
			const ruleSelection = await codeAnalyzer.selectRules(['stubEngine1']);
			// We don't want automated ticking to mess with the messages, so just turn it off for now.
			listener = new SpinnerProgressListener(spyDisplay, -1);

			// The specific targets we use for our workspace don't matter.
			const workspace = await codeAnalyzer.createWorkspace(['package.json']);

			// ==== TESTED BEHAVIOR ====
			// Start listening, then execute the rules, then stop listening.
			listener.listen(codeAnalyzer, ruleSelection);
			await codeAnalyzer.run(ruleSelection, {
				workspace
			});
			listener.stopListening();


			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			// The first event should have been the Spinner Start setting the engine to 0%.
			const startEvent = displayEvents[0];
			expect(startEvent).toHaveProperty('type', DisplayEventType.SPINNER_START);
			expect(startEvent.data).toContain('- stubEngine1 at 0% completion');
			// It's possible (and likely) that there will be some redundancy between the events, for example in cases
			// where the analyzer and engine both separately send a 0% or 100% completion event. To prevent this test from
			// being brittle, we'll just check that the expected percentages were received at least once in the expected order.
			const percentagesInOrder = getUniqueCompletionPercentages(displayEvents.slice(0, displayEvents.length - 1));
			expect(percentagesInOrder).toEqual([0, 50, 100]);
			const endEvent = displayEvents[displayEvents.length - 1];
			expect(endEvent).toHaveProperty('type', DisplayEventType.SPINNER_STOP);
			expect(endEvent.data).toContain('done');
		});


		xit('Properly interleaves progress updates from multiple engines', () => {
			// TODO: Right now, engines are run sequentially. When they start running in parallel, implement this test.
			//  - Use two engines with `setTimeout()` calls timed to interleave with each other.
			//  - Assert that progress in one engine doesn't erase progress in others.
		});

		it('Ticks every ~1000ms, even without updates', async () => {
			// ==== TEST SETUP ====
			//  Assign our standard functional stubs to the core.
			const factory = new StubEnginePluginsFactory_withFunctionalStubEngine();
			for (const enginePlugin of factory.create()) {
				await codeAnalyzer.addEnginePlugin(enginePlugin);
			}
			const ruleSelection = await codeAnalyzer.selectRules(['stubEngine1']);
			// Explicitly use the standard 1-second tick time.
			listener = new SpinnerProgressListener(spyDisplay, 1000);

			// ==== TESTED BEHAVIOR ====
			// Start listening, wait a few seconds, then stop listening.
			listener.listen(codeAnalyzer, ruleSelection);
			await new Promise(res => setTimeout(res, 3500));
			listener.stopListening();

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			// We expect the start event, three ticks, and the stop.
			expect(displayEvents).toHaveLength(5);
			const startEvent = displayEvents[0];
			expect(startEvent).toHaveProperty('type', DisplayEventType.SPINNER_START);
			expect(startEvent.data).toContain('- stubEngine1 at 0% completion');
			for (let i = 0; i < displayEvents.length - 1; i++) {
				// Each of the ticks should have an updated value for the timer.
				expect(displayEvents[i].data).toContain(`after ${i}s`);
			}
			const endEvent = displayEvents[displayEvents.length - 1];
			expect(endEvent).toHaveProperty('type', DisplayEventType.SPINNER_STOP);
			expect(endEvent.data).toContain('done');
		});

		it('Properly interleaves progress updates with ticking', async () => {
			// ==== TEST SETUP ====
			// Use a plugin with an engine that can be configured to wait a certain amount of time between sending its
			// update events.
			const timeablePlugin = new TimeableStubEnginePlugin1();
			await codeAnalyzer.addEnginePlugin(timeablePlugin);
			const engine: TimeableEngine1 = timeablePlugin.getCreatedEngine('timeableEngine1') as TimeableEngine1;
			// Tell it to wait for a relatively small amount of time (less than a second, so the test doesn't take forever).
			engine.setWaitTime(700);
			// The specific files we use for our workspace don't matter.
			const workspace = await codeAnalyzer.createWorkspace(['package.json']);
			const ruleSelection = await codeAnalyzer.selectRules(['all'], {workspace});
			// Set the listener's tick time to be unrealistically low, to make sure ticks happen between engine update events.
			listener = new SpinnerProgressListener(spyDisplay, 200);

			// ==== TESTED BEHAVIOR ====
			// Start listening, execute the rules, and stop listening.
			listener.listen(codeAnalyzer, ruleSelection);
			await codeAnalyzer.run(ruleSelection, {
				workspace
			});
			listener.stopListening();

			// ==== ASSERTIONS ====
			const displayEvents = spyDisplay.getDisplayEvents();
			const startEvent = displayEvents[0];
			expect(startEvent).toHaveProperty('type', DisplayEventType.SPINNER_START);
			expect(startEvent.data).toContain('- timeableEngine1 at 0% completion');
			// It's possible (and likely) that there will be some redundancy between the events, for example in cases
			// where the analyzer and engine both separately send a 0% or 100% completion event. To prevent this test from
			// being brittle, we'll just check that the expected percentages were received at least once in the expected order.
			const percentagesInOrder = getUniqueCompletionPercentages(displayEvents.slice(0, displayEvents.length - 1));
			expect(percentagesInOrder).toEqual([0, 50, 100]);
			const endEvent = displayEvents[displayEvents.length - 1];
			expect(endEvent).toHaveProperty('type', DisplayEventType.SPINNER_STOP);
			expect(endEvent.data).toContain('done');
		}, 10000);

		function getUniqueCompletionPercentages(displayEvents: DisplayEvent[]): number[] {
			const percentMatches = displayEvents.map(e => /(\d+)%/.exec(e.data));
			expect(percentMatches).not.toContain(null);
			const uniquePercentages: Set<number> = new Set();
			percentMatches.forEach(m => uniquePercentages.add(parseInt((m as RegExpExecArray)[0])));
			return [...uniquePercentages.keys()];
		}
	});
});

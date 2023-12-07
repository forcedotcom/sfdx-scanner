import {expect} from 'chai';

import {Lifecycle} from '@salesforce/core';

import {Controller} from '../../src/Controller';
import {Rule, RuleGroup, RuleTarget, TelemetryData} from '../../src/types';
import {ENGINE, CONFIG_FILE, TargetType} from '../../src/Constants';

import {CategoryFilter, EngineFilter, LanguageFilter, RuleFilter, RulesetFilter} from '../../src/lib/RuleFilter';
import {DefaultRuleManager} from '../../src/lib/DefaultRuleManager';
import {OUTPUT_FORMAT, RuleManager, RunOptions} from '../../src/lib/RuleManager';
import {EVENTS, uxEvents} from '../../src/lib/ScannerEvents';

import {RuleCatalog} from '../../src/lib/services/RuleCatalog';
import {RuleEngine} from '../../src/lib/services/RuleEngine';

import {RetireJsEngine} from '../../src/lib/retire-js/RetireJsEngine';
import {SfgeDfaEngine} from '../../src/lib/sfge/SfgeDfaEngine';

import * as TestOverrides from '../test-related-lib/TestOverrides';
import * as TestUtils from '../TestUtils';
import path = require('path');
import Sinon = require('sinon');

import {Messages} from '@salesforce/core';
Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'DefaultRuleManager');

TestOverrides.initializeTestSetup();

let ruleManager: RuleManager = null;
const EMPTY_ENGINE_OPTIONS = new Map<string, string>();

describe('RuleManager', () => {
	let uxSpy;
	let telemetrySpy;

	beforeEach(() => {
		Sinon.createSandbox();
		uxSpy = Sinon.spy(uxEvents, 'emit');
		telemetrySpy = Sinon.spy(Lifecycle.getInstance(), 'emitTelemetry');
	});

	afterEach(() => {
		Sinon.restore();
	});

	before(async () => {
		TestUtils.stubCatalogFixture();

		// Declare our rule manager.
		ruleManager = await Controller.createRuleManager();
	});

	describe('getRulesMatchingCriteria()', () => {
		describe('Test Case: No filters provided', () => {
			it('When no filters are provided, all default-enabled rules are returned', async () => {
				// If we pass an empty list into the method, that's treated as the absence of filter criteria.
				const allRules = await ruleManager.getRulesMatchingCriteria([]);

				// Expect all default-enabled rules to have been returned.
				expect(allRules).to.have.lengthOf(TestUtils.CATALOG_FIXTURE_DEFAULT_ENABLED_RULE_COUNT, 'All rules should have been returned');
			});
		});

		describe('Test Case: Filtering by category only', () => {
			it('Filtering by one category returns only rules in that category', async () => {
				// Set up our filter array.
				const category = 'Best Practices';
				const filters = [
					new CategoryFilter([category]),
					new EngineFilter(['pmd'])];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(2, 'Exactly 2 pmd rules are categorized as "Best Practices".');
				for (const rule of matchingRules) {
					expect(rule.categories).to.contain(category);
				}
		});

			it('Filtering by multiple categories returns any rule in either category', async () => {
				// Set up our filter array.
				const categories = ['Best Practices', 'Design'];
				const filters = [new CategoryFilter(categories)];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(7, 'Exactly 7 rules in enabled engines are categorized as "Best Practices" or "Design"');
				for (const rule of matchingRules) {
					for (const category of rule.categories) {
						expect(categories, JSON.stringify(matchingRules)).to.contain(category);
					}
				}
			});
		});

		describe('Test Case: Filtering by ruleset only', () => {
			it('Filtering by a single ruleset returns only the rules in that ruleset', async () => {
				// Set up our filter array.
				const filters = [new RulesetFilter(['Braces'])];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(3, 'Exactly 8 rules are in the "Braces" ruleset');
			});

			it('Filtering by multiple rulesets returns any rule in either ruleset', async () => {
				// Set up our filter array.
				const filters = [new RulesetFilter(['Braces', 'Best Practices'])];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(6, 'Exactly 6 rules in enabled engines are in the "Braces" or "Best Practices" rulesets');
			});
		});

		describe('Test Case: Filtering by language', () => {
			it('Filtering by a single language returns only rules targeting that language', async () => {
				// Set up our filter array.
				const filters = [new LanguageFilter(['apex'])];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(2, 'There are 2 rules that target Apex');
			});

			it('Filtering by multiple languages returns any rule targeting either language', async () => {
				// Set up our filter array.
				const filters = [new LanguageFilter(['apex', 'javascript'])];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(11, 'There are 11 rules targeting either Apex or JS');
			});
		});

		describe('Test Case: Mixing filter types', () => {
			it('Filtering on multiple columns at once returns only rules that satisfy ALL filters', async () => {
				// Set up our filter array.
				const category = 'Best Practices';
				const filters = [
					new LanguageFilter(['javascript']),
					new CategoryFilter([category])
				];

				// Pass the filter array into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(filters);

				// Expect the right number of rules to be returned.
				expect(matchingRules).to.have.lengthOf(4, 'Exactly 4 rules target Apex and are categorized as "Best Practices".');
				for (const rule of matchingRules) {
					expect(rule.categories).to.contain(category);
				}
			});
		});

		describe('Edge Case: No rules match criteria', () => {
			it('When no rules match the given criteria, an empty list is returned', async () => {
				// Define our preposterous filter array.
				const impossibleFilters = [new CategoryFilter(['beebleborp'])];

				// Pass our filters into the manager.
				const matchingRules = await ruleManager.getRulesMatchingCriteria(impossibleFilters);

				// There shouldn't be anything in the array.
				expect(matchingRules).to.have.lengthOf(0, 'Should be no matching rules');
			});
		});
	});

	describe('runRulesMatchingCriteria()', () => {
		describe('Test Case: Run against test projects', () => {
			before(() => {
				process.chdir(path.join('test', 'code-fixtures', 'projects'));
			});
			after(() => {
				process.chdir("../../..");
			});
			const runOptions: RunOptions = {
				format: OUTPUT_FORMAT.JSON,
				normalizeSeverity: false,
				runDfa: false,
				withPilot: false,
				sfVersion: 'test'
			};
			describe('Test Case: Run without filters', () => {
				it('JS project files', async () => {
					// If we pass an empty list into the method, that's treated as the absence of filter criteria.
					const {results} = await ruleManager.runRulesMatchingCriteria([], ['js'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}

					expect(parsedRes, '' + results).to.be.an("array").that.has.length(1);
					for (const res of parsedRes) {
						expect(res.violations[0], `Message is ${res.violations[0].message}`).to.have.property("ruleName").that.is.not.null;
					}
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('TS project files', async () => {
					// If we pass an empty list into the method, that's treated as the absence of filter criteria.
					const {results} = await ruleManager.runRulesMatchingCriteria([], ['ts'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}

					expect(parsedRes).to.be.an("array").that.has.length(1);
					for (const res of parsedRes) {
						expect(res.violations[0], `Message is ${res.violations[0].message}`).to.have.property("ruleName").that.is.not.null;
					}
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('App project files', async () => {

					// If we pass an empty list into the method, that's treated as the absence of filter criteria.
					const {results} = await ruleManager.runRulesMatchingCriteria([], ['app'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}
					expect(parsedRes).to.be.an("array").that.has.length(8);
					for (const res of parsedRes) {
						expect(res.violations[0], `Message is ${res.violations[0]['message']}`).to.have.property("ruleName").that.is.not.null;
					}
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('All targets match', async () => {
					const validTargets = ['js/**/*.js', 'app/force-app/main/default/classes', '!**/negative-filter-does-not-exist/**'];
					// Set up our filter array.
					const categories = ['Possible Errors'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, validTargets, runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}
					expect(parsedRes).to.be.an("array").that.has.length(1);
					Sinon.assert.callCount(uxSpy, 0);
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Single target file does not match', async () => {
					const invalidTarget = ['does-not-exist.js'];
					// No filters
					const filters = [];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, invalidTarget, runOptions, EMPTY_ENGINE_OPTIONS);

					expect(results).to.equal('[]');
					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, messages.getMessage('warning.targetSkipped', [invalidTarget.join(', ')]));
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Warns correctly if eslint and eslint-lwc have one duplicate path to process', async () => {
					const targets = ['../invalid-lwc'];
					const filters: EngineFilter[] = [new EngineFilter(["eslint-lwc", "eslint", "pmd"])];

					await ruleManager.runRulesMatchingCriteria(filters, targets, runOptions, EMPTY_ENGINE_OPTIONS);

					const filename = path.join(__dirname, '..','code-fixtures', 'invalid-lwc', 'invalidApiDecorator', 'noLeadingUpperCase.js')
					const warningMessage = messages.getMessage('warning.pathsDoubleProcessed', [`${Controller.getSfdxScannerPath()}/${CONFIG_FILE}`, `${filename}`])

					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, warningMessage);
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Warns correctly if eslint and eslint-lwc have one duplicate path to process', async () => {
					const targets = ["js", "../invalid-lwc"];
					const filters: EngineFilter[] = [new EngineFilter(["eslint-lwc", "eslint", "pmd"])];

					await ruleManager.runRulesMatchingCriteria(filters, targets, runOptions, EMPTY_ENGINE_OPTIONS);

					const baseConfigEnv = path.join(__dirname, '..','code-fixtures', 'projects', 'js', 'src', 'baseConfigEnv.js')
					const fileThatUsesQUnit = path.join(__dirname, '..','code-fixtures', 'projects', 'js', 'src', 'fileThatUsesQUnit.js')
					const simpleYetWrong = path.join(__dirname, '..','code-fixtures', 'projects', 'js', 'src', 'simpleYetWrong.js')
					const warningMessage = messages.getMessage('warning.pathsDoubleProcessed', [`${Controller.getSfdxScannerPath()}/${CONFIG_FILE}`, `${baseConfigEnv}, ${fileThatUsesQUnit}, ${simpleYetWrong}, and 1 more`])

					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, warningMessage);
					Sinon.assert.callCount(telemetrySpy, 1);
				});

			});

			describe('Test Case: Run by category', () => {
				it('Filtering by one category runs only rules in that category', async () => {
					// Set up our filter array.
					const category = 'Best Practices';
					const filters = [
						new CategoryFilter([category])];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, ['app'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}

					expect(parsedRes, JSON.stringify(parsedRes)).to.be.an("array").that.has.length(3);
					for (const res of parsedRes) {
						for (const violation of res.violations) {
							expect(violation, `Message is ${violation['message']}`).to.have.property("ruleName").that.is.not.null;
							expect(violation.category).to.equal(category);
						}
					}
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Filtering by multiple categories runs any rule in either category', async () => {
					// Set up our filter array.
					const categories = ['Best Practices', 'Error Prone'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, ['app'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}

					expect(parsedRes).to.be.an("array").that.has.length(6);
					for (const res of parsedRes) {
						expect(res.violations[0], `Message is ${res.violations[0]['message']}`).to.have.property("ruleName").that.is.not.null;
						expect(res.violations[0].category).to.be.oneOf(categories);
					}
					Sinon.assert.callCount(telemetrySpy, 1);
				});
			});

			describe('Test Case: Run by engine', () => {
				it('Filtering by engine works as expected', async () => {
					const engines = [ENGINE.RETIRE_JS, ENGINE.ESLINT];
					const filters = [new EngineFilter(engines)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, ['app'], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== 'string') {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}
					// This result indicates that not all executed engines found violations, which is what we expected. That's fine.
					expect(parsedRes).to.be.an('array').that.has.length(1, 'Wrong number of engines returned violations');
					expect(parsedRes[0].engine).to.equal(ENGINE.ESLINT.valueOf(), 'Wrong engine returned results');
					expect(parsedRes[0].violations.length).to.equal(1, 'Wrong number of violations found');
					Sinon.assert.callCount(telemetrySpy, 1);
					const telemetryArg: TelemetryData = telemetrySpy.args[0][0];
					expect(telemetryArg.eventName).to.equal('ENGINE_EXECUTION');
					expect(telemetryArg.executedEnginesCount).to.equal(2);
					expect(telemetryArg.executedEnginesString).to.equal(JSON.stringify([ENGINE.ESLINT.valueOf(), ENGINE.RETIRE_JS.valueOf()]));
					expect(telemetryArg[ENGINE.PMD.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.PMD_CUSTOM.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.ESLINT.valueOf().toLowerCase()]).to.equal(true);
					expect(telemetryArg[ENGINE.ESLINT_LWC.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.ESLINT_TYPESCRIPT.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.ESLINT_CUSTOM.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.RETIRE_JS.valueOf().toLowerCase()]).to.equal(true);
					expect(telemetryArg[ENGINE.CPD.valueOf().toLowerCase()]).to.equal(false);
					expect(telemetryArg[ENGINE.SFGE.valueOf().toLowerCase()]).to.equal(false);
				});
			})

			describe('Edge Cases', () => {
				it('When no rules match the given criteria, an empty summary is returned', async () => {
					// Define our preposterous filter array.
					const filters = [new CategoryFilter(['beebleborp'])];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, ['app'], runOptions, EMPTY_ENGINE_OPTIONS);
					expect(typeof results).to.equal('string', `Output ${results} should have been a string`);
					expect(results).to.equal('[]', `Output ${results} should have been an empty summary (empty array)`);
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Single target file does not match', async () => {
					const invalidTarget = ['does-not-exist.js'];
					// Set up our filter array.
					const categories = ['Best Practices', 'Error Prone'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, invalidTarget, runOptions, EMPTY_ENGINE_OPTIONS);

					expect(results).to.equal('[]');
					Sinon.assert.callCount(uxSpy, 1);
					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, messages.getMessage("warning.targetSkipped", [invalidTarget.join(', ')]));
					Sinon.assert.callCount(telemetrySpy, 1);
				});


				it('Single target directory does not match', async () => {
					const invalidTarget = ['app/force-app/main/default/no-such-directory'];
					// Set up our filter array.
					const categories = ['Best Practices', 'Error Prone'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, invalidTarget, runOptions, EMPTY_ENGINE_OPTIONS);

					expect(results).to.equal('[]');
					Sinon.assert.callCount(uxSpy, 1);
					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, messages.getMessage("warning.targetSkipped", [invalidTarget.join(', ')]));
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Multiple targets do not match', async () => {
					const invalidTargets = ['does-not-exist-1.js', 'does-not-exist-2.js', 'app/force-app/main/default/no-such-directory'];
					// Set up our filter array.
					const categories = ['Best Practices', 'Error Prone'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, invalidTargets, runOptions, EMPTY_ENGINE_OPTIONS);

					expect(results).to.equal('[]');
					Sinon.assert.callCount(uxSpy, 1);
					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, messages.getMessage("warning.targetsSkipped", [invalidTargets.join(', ')]));
					Sinon.assert.callCount(telemetrySpy, 1);
				});

				it('Some targets do not match', async () => {
					const invalidTargets = ['does-not-exist-1.js', 'does-not-exist-2.js', '**/non-existent/**/*.js', 'app/force-app/main/default/no-such-directory'];
					const validTargets = ['js/**/*.js', '!**/negative-filter-does-not-exist/**'];
					// Set up our filter array.
					const categories = ['Possible Errors'];
					const filters = [new CategoryFilter(categories)];

					const {results} = await ruleManager.runRulesMatchingCriteria(filters, [...invalidTargets, ...validTargets], runOptions, EMPTY_ENGINE_OPTIONS);
					let parsedRes = null;
					if (typeof results !== "string") {
						expect(false, `Invalid output: ${results}`);
					} else {
						parsedRes = JSON.parse(results);
					}
					expect(parsedRes).to.be.an("array").that.has.length(1);
					Sinon.assert.callCount(uxSpy, 1);
					Sinon.assert.calledWith(uxSpy, EVENTS.WARNING_ALWAYS, messages.getMessage('warning.targetsSkipped', [invalidTargets.join(', ')]));
					Sinon.assert.callCount(telemetrySpy, 1);
				});
			});
		});
	});

	describe('unpackTargets()', () => {
		// We want to create a subclass of DefaultRuleManager that exposes the protected method `unpackTargets()`.
		// In order to do that, we'll also need a very basic implementation of RuleCatalog to give to the constructor.
		// That way, we can sidestep any need to mess with the controller or IoC.
		class DummyCatalog implements RuleCatalog {
			getRule(engine: string, ruleName: string): Rule {
				throw new Error('Method not implemented.');
			}

			getRuleGroupsMatchingFilters(filters: RuleFilter[]): Promise<RuleGroup[]> {
				return Promise.resolve([]);
			}

			getRulesMatchingFilters(filters: RuleFilter[]): Rule[] {
				return [];
			}

			init(): Promise<void> {
				return;
			}

		}
		class UnpackTargetsDRM extends DefaultRuleManager {
			constructor() {
				super(new DummyCatalog());
			}

			public async unpackTargets(engine: RuleEngine, targets: string[], matchedTargets: Set<string>): Promise<RuleTarget[]> {
				return super.unpackTargets(engine, targets, matchedTargets);
			}
		}

		describe('Positive matching', () => {
			it('File-type targets are properly matched', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'test/code-fixtures/projects/dep-test-app/folder-a/SomeGenericFile.js', // This file is real and should be included.
					'test/code-fixtures/projects/dep-test-app/folder-e/JsStaticResource1.resource', // This file is also real and should be included.
					'test/code-fixtures/apex/SomeTestClass.cls', // This file is real, but should be excluded since it's Apex.
					'test/beep/boop/not/real.js' // This file isn't real, and shouldn't be included.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());

				// Validate the results.
				expect(results.length).to.equal(2, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[0], 'Wrong file matched');
				expect(results[0].targetType).to.equal(TargetType.FILE, 'Should be matched as file');
				expect(results[0].paths.length).to.equal(1, 'Wrong number of paths matched');
				expect(results[1].target).to.equal(targets[1], 'Wrong file matched');
				expect(results[1].targetType).to.equal(TargetType.FILE, 'Should be matched as file');
				expect(results[1].paths.length).to.equal(1, 'Wrong number of paths matched');
			});

			it('Directory-type targets are properly matched', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'test/code-fixtures/projects/dep-test-app/folder-a', // This directory is real and contains JS files, so it should be included.
					'test/code-fixtures/apex', // This directory is real, but contains only Apex, so should be excluded.
					'test/beep/boop/not/real' // This directory doesn't exist at all, and should be excluded.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());
				// Validate the results.
				expect(results.length).to.equal(1, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[0], 'Wrong directory matched');
				expect(results[0].targetType).to.equal(TargetType.DIRECTORY, 'Should be flagged as directory');
				expect(results[0].paths.length).to.equal(2, 'Wrong number of paths matched');
			});

			it('Positive method-level targets are properly matched', async () => {
				// All tests will use the SFGE engine, since method-level targeting is intended for that engine anyway.
				const engine = new SfgeDfaEngine();
				await engine.init();

				// Targets are all going to be normalized Unix paths, some of which also specify individual methods.
				const targetFile1 = 'test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/AuraEnabledFls.cls';
				const targetMethods1 = ['flsNoEnforcementAttempted', 'flsDoneCorrectly'];
				const targetFile2 = 'test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/VfControllerFls.cls';
				const targetMethods2 = ['flsWrongPermissionChecked'];
				const targets = [
					`${targetFile1}#${targetMethods1.join(';')}`,
					`${targetFile2}#${targetMethods2.join(';')}`
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());

				// Validate the results.
				expect(results.length).to.equal(2, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targetFile1, 'Expected different first file');
				expect(results[0].methods).to.deep.equal(targetMethods1, 'Expected different first methods');
				expect(results[1].target).to.equal(targetFile2, 'Expected different second file');
				expect(results[1].methods).to.deep.equal(targetMethods2, 'Expected different second methods');
			});

			it('Positive glob-type targets are properly matched', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'test/code-fixtures/projects/dep-test-app/**/*Generic*.js', // This glob matches some JS files, and should be included.
					'test/code-fixtures/apex/**/*.cls', // This glob only matches Apex files, so it should be excluded.
					'test/code-fixtures/beep/boop/**/*' // This glob won't match anything at all, so it should be excluded.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());

				// Validate the results.
				expect(results.length).to.equal(1, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[0], 'Wrong glob matched');
				expect(results[0].targetType).to.equal(TargetType.GLOB, 'Should be flagged as glob');
				expect(results[0].paths.length).to.equal(2, 'Wrong number of paths matched');
			});
		});

		describe('Negative matching', () => {
			it('Negative globs properly interact with file targets', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'!**/folder-b/**/*', // This is our negative glob.
					'test/code-fixtures/projects/dep-test-app/folder-a/SomeGenericFile.js', // This file is real and should be included.
					'test/code-fixtures/projects/dep-test-app/folder-b/AnotherGenericFile.js' // This file is real, but matches the negative glob and should be excluded.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());

				// Validate the results.
				expect(results.length).to.equal(1, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[1], 'Wrong file matched');
				expect(results[0].targetType).to.equal(TargetType.FILE, 'Should be flagged as file');
				expect(results[0].paths.length).to.equal(1, 'Wrong number of paths matched');
			});

			it('Negative globs properly interact with directory targets', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'!**/*Static*', // Negative Glob #1
					'!**/*3.5.1.js', // Negative Glob #2
					'test/code-fixtures/projects/dep-test-app/folder-a', // This real directory should be included since no files match negative globs.
					'test/code-fixtures/projects/dep-test-app/folder-b', // This real directory should be included since only some files match negative globs.
					'test/code-fixtures/projects/dep-test-app/folder-e' // This real directory should be excluded since all files match negative globs.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());
				// Validate the results.
				expect(results.length).to.equal(2, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[2], 'Wrong directory matched');
				expect(results[0].targetType).to.equal(TargetType.DIRECTORY, 'Should be flagged as directory');
				expect(results[0].paths.length).to.equal(2, 'Wrong number of paths matched');
				expect(results[1].target).to.equal(targets[3], 'Wrong directory matched');
				expect(results[1].targetType).to.equal(TargetType.DIRECTORY, 'Should be flagged as directory');
				expect(results[1].paths.length).to.equal(1, 'Wrong number of paths matched');
			});

			it('Negative globs properly interact with positive glob targets', async () => {
				// All of the tests will use the RetireJS engine, since it's got the most straightforward inclusion/exclusion rules.
				const engine = new RetireJsEngine();
				await engine.init();

				// Targets are all going to be normalized to Unix paths.
				const targets = [
					'!**/*-3.5.1.js', // Negative Glob #1
					'!**/folder-e/**', // Negative Glob #2
					'test/code-fixtures/projects/dep-test-app/**/*Generic*.js', // This glob should be included since none of its files are excluded by negative globs.
					'test/code-fixtures/projects/dep-test-app/**/jquery*.js', // This glob should be included since only some of its files are excluded by negative globs.
					'test/code-fixtures/projects/dep-test-app/**/*Static*' // This glob should be excluded since all of its files are excluded by negative globs.
				];

				const testRuleManager: UnpackTargetsDRM = new UnpackTargetsDRM();
				await testRuleManager.init();

				// THIS IS THE INVOCATION OF THE TARGET METHOD!
				const results: RuleTarget[] = await testRuleManager.unpackTargets(engine, targets, new Set());

				// Validate the results.
				expect(results.length).to.equal(2, 'Wrong number of targets matched');
				expect(results[0].target).to.equal(targets[2], 'Wrong glob matched');
				expect(results[0].targetType).to.equal(TargetType.GLOB, 'Should be flagged as glob');
				expect(results[0].paths.length).to.equal(2, 'Wrong number of paths matched');
				expect(results[1].target).to.equal(targets[3], 'Wrong glob matched');
				expect(results[1].targetType).to.equal(TargetType.GLOB, 'Should be flagged as glob');
				expect(results[1].paths.length).to.equal(1, 'Wrong number of paths matched');
			});
		});
	});
});

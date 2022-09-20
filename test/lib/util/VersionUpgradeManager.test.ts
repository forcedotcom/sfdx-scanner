import {expect} from 'chai';
import Sinon = require('sinon');
import {VersionUpgradeError, VersionUpgradeManager} from '../../../src/lib/util/VersionUpgradeManager';
import {ConfigContent} from '../../../src/lib/util/Config';
import {ENGINE} from '../../../src/Constants';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {deepCopy} from '../../../src/lib/util/Utils';
import * as TestOverrides from '../../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();

const v2_6_0 = 'v2.6.0';
const v2_6_1 = 'v2.6.1';
const v2_7_0 = 'v2.7.0';
const v2_7_8 = 'v2.7.8';
const v4_3_2 = 'v4.3.2';
const v19_9_9 = 'v19.9.9';
const v22_0_0 = 'v22.0.0';

describe('VersionUpgradeManager', () => {
	describe('Methods', () => {
		// ============ SETUP ===============
		// Create an upgrade manager for successful upgrades, and another where an intermediate upgrade is hardcoded to fail.
		const successfulUpgradeManager = new VersionUpgradeManager();
		const failingUpgradeManager = new VersionUpgradeManager();

		// Casting the managers to `any` lets us override private members.
		const successfulManagerAsAny = successfulUpgradeManager as any;
		const failingManagerAsAny = failingUpgradeManager as any;
		let upgradesCalled = [];

		successfulManagerAsAny.upgradeScriptsByVersion = new Map([
			[v2_6_0, async (c: ConfigContent) => {
				c.javaHome = v2_6_0;
				upgradesCalled.push(v2_6_0);
			}],
			[v2_7_0, async (c: ConfigContent) => {
				c.javaHome = v2_7_0;
				upgradesCalled.push(v2_7_0);
			}],
			[v2_7_8, async (c: ConfigContent) => {
				c.javaHome = v2_7_8;
				upgradesCalled.push(v2_7_8);
			}],
			[v19_9_9, async (c: ConfigContent) => {
				c.javaHome = v19_9_9;
				upgradesCalled.push(v19_9_9);
			}],
			[v22_0_0, async (c: ConfigContent) => {
				c.javaHome = v22_0_0;
				upgradesCalled.push(v22_0_0);
			}]
		]);
		failingManagerAsAny.upgradeScriptsByVersion = new Map([
			[v2_6_1, async (c: ConfigContent) => {
				c.javaHome = v2_6_1;
				upgradesCalled.push(v2_6_1);
			}],
			[v2_7_0, async (c: ConfigContent) => {
				c.javaHome = v2_7_0;
				upgradesCalled.push(v2_7_0);
			}],
			[v2_7_8, async (c: ConfigContent) => {
				c.javaHome = v2_7_8;
				upgradesCalled.push(v2_7_8);
				throw new Error('Hardcoded failure');
			}],
			[v4_3_2, async (c: ConfigContent) => {
				c.javaHome = v4_3_2;
				upgradesCalled.push(v4_3_2);
			}]
		]);
		successfulManagerAsAny.currentVersion = failingManagerAsAny.currentVersion = v22_0_0;

		// ============ TESTS ==================
		describe('#getVersionsBetween()', () => {
			// v2.6.1 and v4.3.2 have no upgrade scripts associated with them in this manager.
			const strictBetween: string[] = successfulManagerAsAny.getVersionsBetween(v2_6_1, v4_3_2);
			// v2.6.0 and v19.9.9 both have upgrade scripts in this manager.
			const onBoundaries: string[] = successfulManagerAsAny.getVersionsBetween(v2_6_0, v19_9_9);
			// v2.7.8 has an upgrade script in this manager.
			const nullFrom: string[] = successfulManagerAsAny.getVersionsBetween(null, v2_7_8);

			it('fromVersion param is exclusive lower bound', () => {
				// Since v2.6.1 has no script, the next-lowest value of 2.7.0 should be used.
				expect(strictBetween[0]).to.equal(v2_7_0, `Lower bound of ${v2_6_1} not respected`);
				// v2.6.0 has a script, but the lower bound is exclusive, so it should use the next-lowest value of 2.7.0.
				expect(onBoundaries[0]).to.equal(v2_7_0, `Lower bound of ${v2_6_0} not properly exclusive`);
			});

			it('toVersion param is inclusive upper bound', () => {
				expect(strictBetween.length).to.equal(2, 'Wrong number of versions returned');
				// v4.3.2 has no script, so the next-highest value of v2.7.8 should be used.
				expect(strictBetween[1]).to.equal(v2_7_8, `Upper bound of ${v4_3_2} not respected`);

				expect(onBoundaries.length).to.equal(3, 'Wrong number of versions returned');
				// v19.9.9 has a script, so that value should be used.
				expect(onBoundaries[2]).to.equal(v19_9_9, `Upper bound of ${v19_9_9} not properly inclusive`);
			});

			it('Null fromVersion is treated as before everything', () => {
				expect(nullFrom[0]).to.equal(v2_6_0, 'Null lower bound not respected');
			});
		});

		describe('#upgrade()', () => {
			it('Only upgrade scripts in the target range are invoked', async () => {
				const testConfig: ConfigContent = {
					currentVersion: null
				};

				// Reset the upgrade tracker.
				upgradesCalled = [];
				// Call the tested method.
				await successfulManagerAsAny.upgrade(testConfig, v2_6_1, v4_3_2);

				// Two versions between the fromVersion and toVersion have upgrade scripts.
				expect(upgradesCalled.length).to.equal(2, 'Wrong number of scripts called');
				expect(upgradesCalled[0]).to.equal(v2_7_0, `Upgrades out of order`);
				expect(upgradesCalled[1]).to.equal(v2_7_8, `Upgrades out of order`);
				// The last script's value should have been persisted.
				expect(testConfig.javaHome).to.equal(v2_7_8, 'Most recent changes should trump all others');
				// Even though the toVersion has no upgrade script, it's what we upgraded to, so that's what the final
				// currentVersion should be.
				expect(testConfig.currentVersion).to.equal(v4_3_2, 'currentVersion should be up-to-date');
			});

			it('Fails safely and cleanly', async () => {
				const testConfig: ConfigContent = {
					currentVersion: null
				};

				// Reset the upgrade tracker.
				upgradesCalled = [];

				try {
					await failingManagerAsAny.upgrade(testConfig, v2_6_0, v19_9_9);
					// It shouldn't be possible to reach this point.
					expect(true).to.equal(false, 'Expected error was never thrown');
				} catch (e) {
					// Four versions within the target range have upgrade scripts, but the third fails, so the fourth
					// should never run.
					expect(upgradesCalled.length).to.equal(3, 'Wrong number of upgrade scripts called');
					expect(upgradesCalled[0]).to.equal(v2_6_1, `Upgrades out of order`);
					expect(upgradesCalled[1]).to.equal(v2_7_0, `Upgrades out of order`);
					expect(upgradesCalled[2]).to.equal(v2_7_8, `Upgrades out of order`);

					// The error should mention the version on which we failed, and include the safe config immediately before.
					expect(e.message).to.include(v2_7_8, `Failed on wrong version`);
					expect((e as VersionUpgradeError).getLastSafeConfig().javaHome).to.equal(v2_7_0, 'Failed changes should be rolled back');
				}
			});
		});

		describe('#upgradeToLatest', () => {
			it('All upgrades after the fromVersion are invoked', async () => {
				const testConfig: ConfigContent = {
					currentVersion: null
				};

				// Reset the upgrade tracker.
				upgradesCalled = [];
				// Call the tested method.
				await successfulManagerAsAny.upgradeToLatest(testConfig, v2_6_1);

				// Five versions are within the target range, but only four of them have an upgrade script attached.
				expect(upgradesCalled.length).to.equal(4, 'Wrong number of scripts called');
				expect(upgradesCalled[0]).to.equal(v2_7_0, `Upgrades out of order`);
				expect(upgradesCalled[1]).to.equal(v2_7_8, `Upgrades out of order`);
				expect(upgradesCalled[2]).to.equal(v19_9_9, `Upgrades out of order`);
				expect(upgradesCalled[3]).to.equal(v22_0_0, `Upgrades out of order`);
				expect(testConfig.currentVersion).to.equal(v22_0_0, 'currentVersion should be up-to-date');
			});
		});
	});

	describe('Individual UpgradeScript tests', () => {
		describe('v2.7.0', () => {
			// Spoof a config that looks like it predates v2.7.0.
			const spoofedConfig: ConfigContent = {
				currentVersion: null,
				engines: [{
					name: ENGINE.RETIRE_JS,
					targetPatterns: [...RetireJsEngine.getSimpleTargetPatterns(), '**/beep/*.js']
				}, {
					name: ENGINE.PMD,
					targetPatterns: ['**/*.apex']
				}]
			};

			// Create an upgrade manager, and use it to fake an upgrade from 2.6.0 to 2.7.0.
			const testManagerAsAny = new VersionUpgradeManager() as any;
			it('RetireJS targetPatterns are removed', async () => {
				await testManagerAsAny.upgrade(spoofedConfig, 'v2.6.0', 'v2.7.0');
				expect(spoofedConfig.engines[0].targetPatterns.length).to.equal(1, 'Wrong number of patterns were preserved');
				expect(spoofedConfig.engines[0].targetPatterns[0]).to.equal('**/beep/*.js', 'Wrong pattern was preserved');

				expect(spoofedConfig.engines[1].targetPatterns.length).to.equal(1, 'Wrong engine was changed');
			});
		});

		describe('v3.0.0', () => {
			// Spoof two configs that look like they predate v3.0.0.
			// The first has a null `currentVersion`, and undefined `disabled` for RetireJS.
			const spoofedConfigWithNull: ConfigContent = {
				currentVersion: null,
				engines: [{
					name: ENGINE.RETIRE_JS,
					targetPatterns: ['**/beep/*.js']
				}, {
					name: ENGINE.PMD,
					targetPatterns: ['**/*.apex']
				}]
			};
			// The second has non-null `currentVersion` and `disabled` for RetireJS.
			const spoofedConfigWithNonNull: ConfigContent = {
				currentVersion: '2.13.1',
				engines: [{
					name: ENGINE.RETIRE_JS,
					targetPatterns: ['**/beep/*.js'],
					disabled: true
				}, {
					name: ENGINE.PMD,
					targetPatterns: ['**/*.apex'],
					disabled: true
				}]
			};

			const testManagerAsAny = new VersionUpgradeManager() as any;
			it('RetireJS is enabled by default', async () => {
				await testManagerAsAny.upgrade(spoofedConfigWithNull, 'v2.13.1', 'v3.0.0');
				await testManagerAsAny.upgrade(spoofedConfigWithNonNull, 'v2.13.1', 'v3.0.0');
				expect(spoofedConfigWithNull.engines[0].disabled).to.equal(false, 'undefined RetireJS disabled value should have been turned to false');
				expect(spoofedConfigWithNull.engines[1].disabled).to.be.undefined; // Undefined PMD disabled value should be left alone.
				expect(spoofedConfigWithNonNull.engines[0].disabled).to.equal(false, 'true RetireJS disabled value should have been turned to false');
				expect(spoofedConfigWithNonNull.engines[1].disabled).to.equal(true, 'true PMD disabled value should be left alone');
			});
		});

		describe('v3.6.0', () => {
			// Spoof a config to use as a fake GA config.
			const spoofedConfig: ConfigContent = {
				engines: [{
					name: ENGINE.RETIRE_JS,
					targetPatterns:['**/beep/*.js'],
					disabled: false
				}, {
					name: ENGINE.PMD,
					targetPatterns: ['**/*.beep'],
					disabled: false
				}]
			};
			// Spoof another config to use as a fake pilot config.
			const spoofedPilotConfig: ConfigContent = {
				// Hardcode the version to a value falling within the pilot window.
				currentVersion: '3.4.0',
				engines: [{
					name: ENGINE.RETIRE_JS,
					targetPatterns:['some/distinctive/filter/criteria/*.js'],
					disabled: true
				}, {
					name: ENGINE.PMD,
					targetPatterns: ['more/distinctive/filtering/*.cls'],
					disabled: true
				}]
			};

			// The tests will be creating some number of Sinon stubs.
			// Those need to be cleaned up afterwards, for safety.
			afterEach(() => {
				Sinon.restore();
			});

			it("GA config is overwritten by more recent pilot config", async () => {
				// Create stubs to pretend that the pilot config exists.
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(spoofedPilotConfig));

				// Make a copy of the base GA config, and set its currentVersion to a value
				// that predates the v3.x pilot.
				const clonedConfig: ConfigContent = deepCopy(spoofedConfig);
				clonedConfig.currentVersion = 'v2.13.9';

				// INVOCATION OF TESTED METHOD
				const testManagerAsAny = new VersionUpgradeManager() as any;
				await testManagerAsAny.upgrade(clonedConfig, clonedConfig.currentVersion, 'v3.6.0');

				// VALIDATIONS
				// The `engines` attribute of the config should be replaced with that of the pilot.
				expect(JSON.stringify(clonedConfig.engines)).to.equal(JSON.stringify(spoofedPilotConfig.engines));
				expect(clonedConfig.currentVersion).to.equal('v3.6.0');
			});


			it("GA config is not overwritten by less recent pilot config", async () => {
				// Create stubs to pretend that the pilot config exists.
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'readFile').resolves(JSON.stringify(spoofedPilotConfig));

				// Make a copy of the base GA config, and set its currentVersion to a value
				// that post-dates the pilot config's.
				const clonedConfig: ConfigContent = deepCopy(spoofedConfig);
				clonedConfig.currentVersion = 'v3.5.0';

				// INVOCATION OF TESTED METHOD
				const testManagerAsAny = new VersionUpgradeManager() as any;
				await testManagerAsAny.upgrade(clonedConfig, clonedConfig.currentVersion, 'v3.6.0');

				// VALIDATIONS
				// The `engines` attribute of the config should be unchanged.
				expect(JSON.stringify(clonedConfig.engines)).to.equal(JSON.stringify(spoofedConfig.engines));
				expect(clonedConfig.currentVersion).to.equal('v3.6.0');
			});

			it("Non-existing pilot config causes no issues", async () => {
				// We want to pretend that no pilot config exists.
				Sinon.stub(FileHandler.prototype, 'exists').resolves(false);

				// Make a copy of the base GA config and attempt to upgrade it.
				const clonedConfig: ConfigContent = deepCopy(spoofedConfig);
				// INVOCATION OF TESTED METHOD
				const testManagerAsAny = new VersionUpgradeManager() as any;
				await testManagerAsAny.upgrade(clonedConfig, 'v2.13.1', 'v3.6.0');

				// VALIDATION
				// The engines should be unchanged.
				expect(JSON.stringify(clonedConfig.engines)).to.equal(JSON.stringify(spoofedConfig.engines));
				expect(clonedConfig.currentVersion).to.equal('v3.6.0');
			});
		});
	});
});

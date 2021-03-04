import {expect} from 'chai';
import {VersionUpgradeManager} from '../../../src/lib/util/VersionUpgradeManager';
import {ConfigContent} from '../../../src/lib/util/Config';
import {ENGINE} from '../../../src/Constants';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine';

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
			[v2_6_0, async () => {upgradesCalled.push(v2_6_0)}],
			[v2_7_0, async () => {upgradesCalled.push(v2_7_0)}],
			[v2_7_8, async () => {upgradesCalled.push(v2_7_8)}],
			[v19_9_9, async () => {upgradesCalled.push(v19_9_9)}],
			[v22_0_0, async () => {upgradesCalled.push(v22_0_0)}]
		]);
		failingManagerAsAny.upgradeScriptsByVersion = new Map([
			[v2_6_1, async () => {upgradesCalled.push(v2_6_1)}],
			[v2_7_0, async () => {upgradesCalled.push(v2_7_0)}],
			[v2_7_8, async () => {throw new Error('Hardcoded failure')}],
			[v4_3_2, async () => {upgradesCalled.push(v4_3_2)}]
		]);
		successfulManagerAsAny.currentVersion = failingManagerAsAny.currentVersion = v22_0_0;

		// ============ TESTS ==================
		describe('#getVersionsBetween()', () => {
			const strictBetween: string[] = successfulManagerAsAny.getVersionsBetween(v2_6_1, v4_3_2);
			const onBoundaries: string[] = successfulManagerAsAny.getVersionsBetween(v2_6_0, v19_9_9);
			const nullFrom: string[] = successfulManagerAsAny.getVersionsBetween(null, v2_7_8);

			it('fromVersion param is exclusive lower bound', () => {
				expect(strictBetween[0]).to.equal(v2_7_0, `Lower bound of ${v2_6_1} not respected`);
				expect(onBoundaries[0]).to.equal(v2_7_0, `Lower bound of ${v2_6_0} not properly exclusive`);
			});

			it('toVersion param is inclusive upper bound', () => {
				expect(strictBetween.length).to.equal(2, 'Wrong number of versions returned');
				expect(strictBetween[1]).to.equal(v2_7_8, `Upper bound of ${v4_3_2} not respected`);

				expect(onBoundaries.length).to.equal(3, 'Wrong number of versions returned');
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

				// Three versions are within the target range, but only two of them have an upgrade script attached.
				expect(upgradesCalled.length).to.equal(2, 'Wrong number of scripts called');
				expect(upgradesCalled[0]).to.equal(v2_7_0, `Upgrades out of order`);
				expect(upgradesCalled[1]).to.equal(v2_7_8, `Upgrades out of order`);
				expect(testConfig.currentVersion).to.equal(v4_3_2, 'currentVersion should be up-to-date');
			});

			it('Upgrade scripts are atomic', async () => {
				const testConfig: ConfigContent = {
					currentVersion: null
				};

				// Reset the upgrade tracker.
				upgradesCalled = [];
				try {
					await failingManagerAsAny.upgrade(testConfig, v2_6_0, v19_9_9);
					expect(true).to.equal(false, 'Error should be thrown before this line is reached');
				} catch (e) {
					// Three versions fall within the target range, but since the third script fails, only the first two
					// should be executed.
					expect(upgradesCalled.length).to.equal(2, `Wrong number of scripts called`);
					expect(upgradesCalled[0]).to.equal(v2_6_1, `Upgrades out of order`);
					expect(upgradesCalled[1]).to.equal(v2_7_0, `Upgrades out of order`);
					expect(e.message).to.include(v2_7_8, `Failed on wrong version`);
					expect(testConfig.currentVersion).to.equal(v2_7_0, `Rolled back to wrong version`);
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
	})
});

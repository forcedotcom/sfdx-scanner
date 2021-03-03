import {expect} from 'chai';
import {VersionUpgradeManager} from '../../../src/lib/util/VersionUpgradeManager';

const v2_6_0 = 'v2.6.0';
const v2_6_1 = 'v2.6.1';
const v2_7_0 = 'v2.7.0';
const v2_7_8 = 'v2.7.8';
const v4_3_2 = 'v4.3.2';
const v19_9_9 = 'v19.9.9';
const v22_0_0 = 'v22.0.0';

describe('VersionUpgradeManager', () => {
	// ============ SETUP ===============
	const testUpgradeManager = new VersionUpgradeManager();

	// Casting the manager to `any` lets us override private members.
	const testAsAny = testUpgradeManager as any;
	let upgradesCalled: string[] = [];
	testAsAny.upgradeScriptsByVersion = new Map([
		[v2_6_0, async () => {upgradesCalled.push(v2_6_0)}],
		[v2_7_0, async () => {upgradesCalled.push(v2_7_0)}],
		[v2_7_8, async () => {upgradesCalled.push(v2_7_8)}],
		[v19_9_9, async () => {upgradesCalled.push(v19_9_9)}],
		[v22_0_0, async () => {upgradesCalled.push(v22_0_0)}]
	]);
	testAsAny.currentVersion = v22_0_0;

	// ============ TESTS ==================
	describe('#getVersionsBetween()', () => {
		const strictBetween: string[] = testAsAny.getVersionsBetween(v2_6_1, v4_3_2);
		const onBoundaries: string[] = testAsAny.getVersionsBetween(v2_6_0, v19_9_9);
		const nullFrom: string[] = testAsAny.getVersionsBetween(null, v2_7_8);

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
			// Reset the upgrade tracker.
			upgradesCalled = [];
			// Call the tested method.
			await testAsAny.upgrade(v2_6_1, v2_7_8);

			// Three versions are within the target range, but only two of them have an upgrade script attached.
			expect(upgradesCalled.length).to.equal(2, 'Wrong number of scripts called');
			expect(upgradesCalled[0]).to.equal(v2_7_0, `Upgrades out of order`);
			expect(upgradesCalled[1]).to.equal(v2_7_8, `Upgrades out of order`);
		});
	});

	describe('#upgradeToLatest', () => {
		it('All upgrades after the fromVersion are invoked', async () => {
			// Reset the upgrade tracker.
			upgradesCalled = [];
			// Call the tested method.
			await testAsAny.upgradeToLatest(v2_6_1);

			// Five versions are within the target range, but only four of them have an upgrade script attached.
			expect(upgradesCalled.length).to.equal(4, 'Wrong number of scripts called');
			expect(upgradesCalled[0]).to.equal(v2_7_0, `Upgrades out of order`);
			expect(upgradesCalled[1]).to.equal(v2_7_8, `Upgrades out of order`);
			expect(upgradesCalled[2]).to.equal(v19_9_9, `Upgrades out of order`);
			expect(upgradesCalled[3]).to.equal(v22_0_0, `Upgrades out of order`);
		});
	});
});

// ================ IMPORTS ===================
import semver = require('semver');
import {Messages} from '@salesforce/core';
import {ConfigContent, EngineConfigContent} from './Config';
import {ENGINE} from '../../Constants';
import {RetireJsEngine} from '../retire-js/RetireJsEngine';
import {deepCopy} from './Utils';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// ================ TYPES =====================
type VersionUpgradeScript = (config?: ConfigContent) => Promise<void>;


// ================ CONSTANTS =================

const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'VersionUpgradeManager');

// RULES FOR WRITING UPGRADE SCRIPTS:
// - Ideally, your script should require no parameters beyond the ConfigContent object. If more parameters must be added,
//   they should be sufficiently generic that providing them to other scripts isn't troublesome.
// - You can assume that every previous upgrade script was successfully executed.
// - Make sure that if your script fails, it fails cleanly. Roll back changes, and provide a clear error message.
const upgradeScriptsByVersion: Map<string, VersionUpgradeScript> = new Map();
upgradeScriptsByVersion.set('v2.7.0', (config: ConfigContent): Promise<void> => {
	// v2.7.0 adds advanced target patterns and generally enhances the RetireJS integration, and some of the patterns
	// that used to be in the config are now hardcoded into the engine. The config's targetPatterns array should be
	// scoured of any such patterns. Other patterns should be permitted to stay, since they might be meaningful.
	const retireJsConfig: EngineConfigContent = config.engines.find(e => e.name === ENGINE.RETIRE_JS);
	if (retireJsConfig) {
		const hardcodedPatterns: Set<string> = new Set(RetireJsEngine.getSimpleTargetPatterns());
		retireJsConfig.targetPatterns = retireJsConfig.targetPatterns.filter(s => !hardcodedPatterns.has(s));
	}
	return Promise.resolve();
});
upgradeScriptsByVersion.set('v3.0.0', (config: ConfigContent): Promise<void> => {
	// In v3.0.0, we're changing RetireJS from a supplemental engine that must be manually enabled to an enabled-byu-default
	// engine. So we need to change its `disabled` config value from true to false.
	const retireJsConfig: EngineConfigContent = config.engines.find(e => e.name === ENGINE.RETIRE_JS);
	if (retireJsConfig) {
		retireJsConfig.disabled = false;
	}
	return Promise.resolve();
});


// ================ CLASSES =====================
export class VersionUpgradeError extends Error {
	private readonly lastSafeConfig: ConfigContent;

	constructor(message: string, lastSafeConfig: ConfigContent) {
		super(message);
		this.lastSafeConfig = lastSafeConfig;
	}

	public getLastSafeConfig(): ConfigContent {
		return this.lastSafeConfig;
	}
}

export class VersionUpgradeManager {
	private readonly currentVersion: string;
	private upgradeScriptsByVersion: Map<string,VersionUpgradeScript>;

	constructor() {
		// It's typically bad practice to use `require` instead of `import`, but the former is much more straightforward
		// in this case.
		// eslint-disable-next-line @typescript-eslint/no-var-requires, @typescript-eslint/no-unsafe-member-access, @typescript-eslint/no-unsafe-assignment
		this.currentVersion = require('../../../package.json').version;
		this.upgradeScriptsByVersion = upgradeScriptsByVersion;
	}

	public upgradeRequired(configVersion: string): boolean {
		return !configVersion || (semver.lt(configVersion, this.currentVersion));
	}

	private getVersionsBetween(fromVersion: string, toVersion: string): string[] {
		const orderedVersions = semver.sort([...this.upgradeScriptsByVersion.keys()]);
		return orderedVersions.filter((v: string) => {
			// The lower bound is EXCLUSIVE, and a null value is treated as below non-null value.
			const afterFromVersion = !fromVersion || semver.lt(fromVersion, v);
			// The upper bound is INCLUSIVE.
			const atOrBeforeToVersion = semver.lte(v, toVersion);
			return afterFromVersion && atOrBeforeToVersion;
		});
	}

	private async upgrade(config: ConfigContent, fromVersion: string, toVersion: string): Promise<void> {
		// We want to sequentially apply all of the upgrade scripts between the fromVersion and the toVersion, so we'll
		// need to get all of those versions.
		const versions: string[] = this.getVersionsBetween(fromVersion, toVersion);

		// Handle each upgrade script in sequence.
		for (const version of versions) {
			// Store the config as it currently exists.
			const existingConfig = deepCopy(config);
			try {
				await this.upgradeScriptsByVersion.get(version)(config);
			} catch (e) {
				// If the script failed, prefix the error so it's clear where it came from, then throw a new error with
				// the prefixed message and the last safe configuration.
				const message: string = e instanceof Error ? e.message : e as string;
				throw new VersionUpgradeError(messages.getMessage('upgradeFailed', [version, message]), existingConfig);
			}
			// If we're here, we're considered to have successfully upgraded to this version. So we'll update the config
			// to reflect that.
			config.currentVersion = version;
		}
		// If we successfully run all of the upgrade scripts, we can upgrade directly to the toVersion and be done.
		config.currentVersion = toVersion;
	}

	public async upgradeToLatest(config: ConfigContent, fromVersion: string): Promise<void> {
		await this.upgrade(config, fromVersion, this.currentVersion);
	}
}

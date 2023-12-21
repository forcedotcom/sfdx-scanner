// ================ IMPORTS ===================
import semver = require('semver');
import path = require('path');
import {ConfigContent, EngineConfigContent} from './Config';
import {ENGINE, CONFIG_PILOT_FILE} from '../../Constants';
import {RetireJsEngine} from '../retire-js/RetireJsEngine';
import {deepCopy} from './Utils';
import {FileHandler} from './FileHandler';
import { Controller } from '../../Controller';
import {Bundle, getMessage} from "../../MessageCatalog";

// ================ TYPES =====================
type VersionUpgradeScript = (config?: ConfigContent) => Promise<void>;


// ================ CONSTANTS =================

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
	// In v3.0.0, we're changing RetireJS from a supplemental engine that must be manually enabled to an enabled-by-default
	// engine. So we need to change its `disabled` config value from true to false.
	const retireJsConfig: EngineConfigContent = config.engines.find(e => e.name === ENGINE.RETIRE_JS);
	if (retireJsConfig) {
		retireJsConfig.disabled = false;
	}
	return Promise.resolve();
});
upgradeScriptsByVersion.set('v3.6.0', async (config: ConfigContent): Promise<void> => {
	// v3.6.0 is the first v3.x version that's GA instead of a pilot release.
	// To provide pilot users with a continuity of behavior, we want to overwrite
	// the existing GA config with the pilot config, if one exists and it is deemed
	// appropriate to do so.
	const fh: FileHandler = new FileHandler();
	const pilotConfigPath = path.join(Controller.getSfdxScannerPath(), CONFIG_PILOT_FILE);
	if (await fh.exists(pilotConfigPath)) {
		// If the pilot config exists, read it in.
		const pilotConfig: ConfigContent = JSON.parse(await fh.readFile(pilotConfigPath)) as ConfigContent;
		// If the pilot config is for a later plug-in version than the current GA config, replace
		// the GA config's `engines` property with that of the pilot config.
		if (!config.currentVersion || semver.lt(config.currentVersion, pilotConfig.currentVersion)) {
			config.engines = pilotConfig.engines;
		}
	}
});
upgradeScriptsByVersion.set('v3.17.0', (config: ConfigContent): Promise<void> => {
	// In v3.17.0, we're changing PMD's config so that it no longer excludes Salesforce metadata
	// files by default. This will automatically apply to any newly-generated configs, but we also
	// want to retroactively remove this exclusion for existing users.
	const pmdConfig: EngineConfigContent = config.engines.find(e => e.name === ENGINE.PMD);
	if (pmdConfig) {
		pmdConfig.targetPatterns = pmdConfig.targetPatterns.filter(s => s !== '!**/*-meta.xml');
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

	/**
	 * Depending on what version we're upgrading from, we may want to force the creation
	 * of a back-up config, instead of only creating one in response to failure.
	 * This method indicates whether that's the case.
	 * @param configVersion
	 * @returns true if a back-up should always be created, else false
	 */
	public versionNecessitatesBackup(configVersion: string): boolean {
		// Always back up config from previous major versions.
		return !configVersion || (semver.major(configVersion) < semver.major(this.currentVersion));
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
				throw new VersionUpgradeError(getMessage(Bundle.VersionUpgradeManager, 'upgradeFailed', [version, message]), existingConfig);
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

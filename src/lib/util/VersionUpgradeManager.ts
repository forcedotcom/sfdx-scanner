// ================ IMPORTS ===================
import semver = require('semver');

// ================ TYPES =====================
type VersionUpgradeScript = () => Promise<void>;


// ================ CONSTANTS =================

// RULES FOR WRITING UPGRADE SCRIPTS:
// - You're allowed to use Controller.getConfig() if you need, but avoid calling Config.writeConfig(), since the config
//   will call that method for you after all the scripts run.
// - Ideally, your script should require no parameters. If parameters must be added, they should be sufficiently generic
//   that providing them to other scripts isn't troublesome.
// - You can assume that every previous upgrade script was successfully executed.
// - Make sure that if your script fails, it fails cleanly. Roll back changes, and provide a clear error message.
const upgradeScriptsByVersion: Map<string, VersionUpgradeScript> = new Map();


// ================ CLASSES =====================
export class VersionUpgradeError extends Error {
	private readonly version: string;

	constructor(msg: string, version: string) {
		super(msg);
		this.version = version;
	}

	public getLastSuccessfulVersion(): string {
		return this.version;
	}
}

export class VersionUpgradeManager {
	private readonly currentVersion: string;
	private upgradeScriptsByVersion: Map<string,VersionUpgradeScript>;

	constructor() {
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

	private async upgrade(fromVersion: string, toVersion: string): Promise<void> {
		// We want to sequentially apply all of the upgrade scripts between the fromVersion and the toVersion, so we'll
		// need to get all of those versions.
		const versions: string[] = this.getVersionsBetween(fromVersion, toVersion);

		// We want to keep track of the latest version whose upgrade script ran successfully, for error-handling purposes.
		let lastSuccessfulVersion = null;

		// Handle each upgrade script in sequence.
		for (const version of versions) {
			if (this.upgradeScriptsByVersion.has(version)) {
				try {
					await this.upgradeScriptsByVersion.get(version)();
				} catch (e) {
					// If the upgrade script fails, prefix the error so it's clear where it came from.
					throw new VersionUpgradeError(`Upgrade script for ${version} failed: ${e.message ||e }`, lastSuccessfulVersion);
				}
			}
			// If we're here, we're considered to have successfully upgraded to this version.
			lastSuccessfulVersion = version;
		}
	}

	public async upgradeToLatest(fromVersion: string): Promise<string> {
		await this.upgrade(fromVersion, this.currentVersion);
		return this.currentVersion;
	}
}

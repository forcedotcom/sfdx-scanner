import {Logger, SfError} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {Controller} from '../Controller';

import process = require('process');
import findJavaHome from 'find-java-home';
import childProcess = require('child_process');
import path = require('path');
import {FileHandler} from './util/FileHandler';
import {Config} from './util/Config';
import {CONFIG_FILE} from '../Constants';
import {BundleName, getMessage} from "../MessageCatalog";

const JAVA_HOME_SYSTEM_VARIABLES = ['JAVA_HOME', 'JRE_HOME', 'JDK_HOME'];

// Exported only to be used by tests
export class JreSetupManagerDependencies {
	async autoDetectJavaHome(): Promise<string> {
		return new Promise<string>((resolve) => {
			// Returning a void to show that we don't need to handle reject in this case.
			// If this gets rejected, we'll simply move on to the next step.
			void findJavaHome({allowJre: true}, (err, home) => {
				resolve(err || (typeof home != 'string') ? null : home);
			});
		});
	}
}

class JreSetupManager extends AsyncCreatable {
	private logger!: Logger;
	private config!: Config;
	private configFile: string;
	private dependencies: JreSetupManagerDependencies;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('verifyJRE');

		this.config = await Controller.getConfig();
		this.configFile = path.join(Controller.getSfdxScannerPath(), CONFIG_FILE)
		this.dependencies = new JreSetupManagerDependencies();

		this.initialized = true;
	}

	async verifyJreSetup(): Promise<string> {
		// Find Java Home
		const javaHome = await this.findJavaHome();

		// If we have javaHome, verify that it is a valid path
		await this.verifyPath(javaHome);

		// If path exists, verify Java version
		await this.verifyJavaVersion(javaHome);

		// Write javaHome to Config file
		await this.config.setJavaHome(javaHome);

		return javaHome;
	}

	private async findJavaHome(): Promise<string> {
		let javaHome: string;
		// First try getting javaHome from config
		javaHome = this.config.getJavaHome();

		// If config doesn't have javaHome, try getting from System Variables
		if (!javaHome) {
			javaHome = this.findJavaHomeFromSysVariables();
		}

		// If System Variables don't have javaHome, try to detect automatically from javac
		if (!javaHome) {
			javaHome = await this.autoDetectJavaHome();
		}

		// If we reach this point and we somehow still haven't found a javaHome, then we're pretty thoroughly hosed.
		// So we'll just throw an error telling the user to set it themselves.
		if (!javaHome) {
			const errName = 'NoJavaHomeFound';
			throw new SfError(getMessage(BundleName.JreSetupManager, errName, [this.configFile]), errName);
		}

		return javaHome;
	}

	private findJavaHomeFromSysVariables(): string {
		let javaHome: string = null;
		const env = process.env;
		for (const sysVariable of JAVA_HOME_SYSTEM_VARIABLES) {
			javaHome = env[sysVariable];
			if (javaHome) {
				this.logger.trace(`Found javaHome from ${sysVariable} as ${javaHome}`);
				return javaHome;
			}
		}
		return javaHome;
	}

	private autoDetectJavaHome(): Promise<string> {
		return this.dependencies.autoDetectJavaHome();
	}

	private async verifyPath(javaHome: string): Promise<void> {
		const fileHandler = new FileHandler();
		try {
			await fileHandler.stats(javaHome);
		} catch (e) {
			const error: NodeJS.ErrnoException = e as NodeJS.ErrnoException;
			const errName = 'InvalidJavaHome';
			throw new SfError(getMessage(BundleName.JreSetupManager, errName, [javaHome, error.code, this.configFile]), errName);
		}
	}

	private async verifyJavaVersion(javaHome: string): Promise<void> {
		const versionCommandOut = await this.fetchJavaVersion(javaHome);

		// We are using "java -version" below which has output that typically looks like:
		// * (from MacOS): "openjdk version "11.0.6" 2020-01-14 LTS\nOpenJDK Runtime Environment Zulu11.37+17-CA (build 11.0.6+10-LTS)\nOpenJDK 64-Bit Server VM Zulu11.37+17-CA (build 11.0.6+10-LTS, mixed mode)\n"
		// From much research it should ideally say "version " and then either a number with or without quotes.
		// If instead we used java --version then the output would look something like:
		// * (from Win10): "openjdk 14 2020-03-17\r\nOpenJDK Runtime Environment (build 14+36-1461)\r\nOpenJDK 64-Bit Server VM (build 14+36-1461, mixed mode, sharing)\r\n"
		// Notice it doesn't have the word "version" but again, we don't call "--version". But for sanity sakes,
		// we will attempt to support this as well. Basically we want to get the "11.0.6" or "14" part.

		// First we'll see if the word "version" exists with the version number and use that first.
		const matchedParts = versionCommandOut.match(/version\s+"?(\d+(\.\d+)*)"?/i);
		this.logger.trace(`Attempt 1: Java version output match results is ${JSON.stringify(matchedParts)}`);
        let version: string = "";
		if (matchedParts && matchedParts.length > 1) {
			version = matchedParts[1];
		} else {
			// Otherwise we'll try to get the version number the old way just be looking for the first number
			const matchedParts = versionCommandOut.match(/\s+(\d+(\.\d+)*)/);
			this.logger.trace(`Attempt 2: Java version output match results is ${JSON.stringify(matchedParts)}`);
			if (!matchedParts || matchedParts.length < 2) {
				throw new SfError(getMessage(BundleName.JreSetupManager, 'VersionNotFound', [this.configFile]));
			}
			version = matchedParts[1];
		}

		// Up to JDK8, the version scheme is 1.blah
		// Starting JDK 9, the version scheme is 9.blah for 9, 10.blah for 10, etc.
		const majorVersion = parseInt(version.split('.')[0]);

		if (majorVersion < 11) {
			// Not matching what we are looking for
			const errName = 'InvalidVersion';
			throw new SfError(getMessage(BundleName.JreSetupManager, errName, [version, this.configFile]), errName);
		}

		this.logger.trace(`Java version found as ${version}`);
		return;
	}

	private async fetchJavaVersion(javaHome: string): Promise<string> {
		const javaWithFullPath = path.join(javaHome, 'bin', 'java');
		// Run java -version and examine stderr for version
		return new Promise<string>((resolve, reject) => {
			childProcess.execFile(javaWithFullPath, ['-version'], {},
				(error, _stdout, stderr) => {
					if (error) {
						return reject(`Could not fetch Java version from path ${javaHome}. Reason: ${error.message}`);
					}
					// TODO: Mac always returns this value in stderr. Check if the behavior is the same for other OS types
					this.logger.trace(`Java version received as ${stderr}`);
					return resolve(stderr);
				});
		});
	}
}

// Method that'll be invoked externally
export async function verifyJreSetup(): Promise<string> {
	const manager = await JreSetupManager.create({});
	return await manager.verifyJreSetup();
}

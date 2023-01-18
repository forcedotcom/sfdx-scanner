import {Logger, SfError, Messages} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {Controller} from '../Controller';

import process = require('process');
import * as findJavaHome from 'find-java-home';
import childProcess = require('child_process');
import path = require('path');
import {FileHandler} from './util/FileHandler';
import {Config} from './util/Config';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'jreSetupManager');

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
	private dependencies: JreSetupManagerDependencies;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('verifyJRE');

		this.config = await Controller.getConfig();
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
			throw new SfError(messages.getMessage(errName, []), errName);
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
			throw new SfError(messages.getMessage(errName, [javaHome, error.code]), errName);
		}
	}

	private async verifyJavaVersion(javaHome: string): Promise<void> {
		const versionOut = await this.fetchJavaVersion(javaHome);

		// Version output looks like this:
		// MacOS: "openjdk version "11.0.6" 2020-01-14 LTS\nOpenJDK Runtime Environment Zulu11.37+17-CA (build 11.0.6+10-LTS)\nOpenJDK 64-Bit Server VM Zulu11.37+17-CA (build 11.0.6+10-LTS, mixed mode)\n"
		// Win10: "openjdk 14 2020-03-17\r\nOpenJDK Runtime Environment (build 14+36-1461)\r\nOpenJDK 64-Bit Server VM (build 14+36-1461, mixed mode, sharing)\r\n"
		// We want to get the "11.0" or "14" part
		// The version number could be of the format 11.0 or 1.8 or 14
		const regex = /(\d+)(\.(\d+))?/;
		const matchedParts = regex.exec(versionOut);
		this.logger.trace(`Version output match for pattern ${regex.toString()} is ${JSON.stringify(matchedParts)}`);

		// matchedParts should have four groups: "11.0", "11", ".0", "0" or "14", "14", undefined, undefined
		if (!matchedParts || matchedParts.length < 4) {
			throw new SfError(messages.getMessage('VersionNotFound', []));
		}

		const majorVersion = parseInt(matchedParts[1]);
		const minorVersion = matchedParts[3] ? parseInt(matchedParts[3]) : '';
		let version = '';
		// We want to allow 1.8 and greater.
		// Upto JDK8, the version scheme is 1.blah
		// Starting JDK 9, the version scheme is 9.blah for 9, 10.blah for 10, etc.
		// If either version part clicks, we should be good.
		if (majorVersion >= 9) {
			// Accept if Major version is greater than or equal to 9
			version = `${majorVersion}${minorVersion ? `.${minorVersion}` : ''}`;
		} else if (majorVersion === 1 && minorVersion === 8) {
			// Accommodating 1.8
			version = `${majorVersion}.${minorVersion}`;
		} else {
			// Not matching what we are looking for
			const errName = 'InvalidVersion';
			throw new SfError(messages.getMessage(errName, [version]), errName);
		}

		this.logger.trace(`Java version found as ${version}`);
		return;


	}

	private async fetchJavaVersion(javaHome: string): Promise<string> {
		const javaWithFullPath = path.join(javaHome, 'bin', 'java');
		// Run java -version and examine stderr for version
		return new Promise((resolve, reject) => {
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

import { CONFIG } from '../Constants';
import { ConfigFile, Logger, SfdxError, Messages } from '@salesforce/core';
import { AsyncCreatable } from '@salesforce/kit';

import process = require('process');
import findJavaHome = require('find-java-home');
import childProcess = require('child_process');
import path = require('path');
import { FileHandler } from './FileHandler';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

const JAVA_HOME_KEY = 'java-home';
const JAVA_HOME_SYSTEM_VARIABLES = ['JAVA_HOME', 'JRE_HOME', 'JDK_HOME'];

// Exported to be used by tests. If this is needed to be used in other places, 
// consider moving it to a module of its own
export class Config extends ConfigFile<ConfigFile.Options> {

    public static getFileName(): string {
        // TODO: Revisit the file location!
        // This doesn't work if I give the directory as ~/.sfdx-scanner.
        // File only goes to ~/.sfdx
        return CONFIG;
    }
}

// Exported only to be used by tests
export class JreSetupManagerDependencies {
    autoDetectJavaHome(handleDetectedValue: (err: Error, home: string) => string): Promise<string> {
        return new Promise<string>((resolve) => {
            findJavaHome({allowJre: true}, (err, home) => {
                resolve(handleDetectedValue(err, home));
            });
        });
    }
}

class JreSetupManager extends AsyncCreatable {
    private logger!: Logger;
    private config!: Config;
    private dependencies: JreSetupManagerDependencies;

    protected async init(): Promise<void> {
        this.logger = await Logger.child('verifyJRE');
        this.config = await Config.create({
            isGlobal: true,
            throwOnNotFound: false
        });
        this.dependencies = new JreSetupManagerDependencies();
    }

    async verifyJreSetup(): Promise<string> {
        // Find Java Home
        const javaHome = await this.findJavaHome();

        // If we have javaHome, verify that it is a valid path
        await this.verifyPath(javaHome);

        // If path exists, verify Java version
        await this.verifyJavaVersion(javaHome);

        // Write javaHome to Config file
        await this.setJavaHomeInConfig(javaHome);

        return javaHome;
    }

    private async findJavaHome(): Promise<string> {
        let javaHome: string;
        // First try getting javaHome from config
        javaHome = await this.getJavaHomeFromConfig();

        // If config doesn't have javaHome, try getting from System Variables
        if (!javaHome) {
            javaHome = this.findJavaHomeFromSysVariables();
        }

        // If System Variables don't have javaHome, try to detect automatically from javac
        if (!javaHome) {
            javaHome = await this.autoDetectJavaHome();
        }

        // At this point, if we don't have a javaHome, we throw an error and ask user to add it themselves
        if (!javaHome) {
            throw SfdxError.create('@salesforce/sfdx-scanner', 'jreSetupManager', 'NoJavaHomeFound', []);
        }

        return javaHome;
    }

    private findJavaHomeFromSysVariables(): string {
        let javaHome = null;
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
        return this.dependencies.autoDetectJavaHome(this.handleAutoDetectedJavaHome);
    }
    handleAutoDetectedJavaHome(err: Error, home: string): string {
        if (err) {
            return null; // Absence of javaHome is handled later
        }
        else {
            return home;
        }
    }

    private async verifyPath(javaHome: string): Promise<void> {
        const fileHandler = new FileHandler();
        try {
            await fileHandler.stats(javaHome);
        } catch (error) {
            throw SfdxError.create('@salesforce/sfdx-scanner', 'jreSetupManager', 'InvalidJavaHome', [javaHome]);
        }
    }

    private async verifyJavaVersion(javaHome: string): Promise<void> {
        const versionOut = await this.fetchJavaVersion(javaHome);

        // Version output looks like this:
        // "openjdk version "11.0.6" 2020-01-14 LTS\nOpenJDK Runtime Environment Zulu11.37+17-CA (build 11.0.6+10-LTS)\nOpenJDK 64-Bit Server VM Zulu11.37+17-CA (build 11.0.6+10-LTS, mixed mode)\n"
        // We want to get the "version \"11.0" part
        // The version number could be of the format 11.0 or 1.8
        const regex = 'version "(\\d+).(\\d+)';
        const matchedParts = versionOut.match(regex);
        this.logger.trace(`Version output match for pattern ${regex} is ${matchedParts}`);

        // matchedParts should have three groups: "version \"11.0", "11", "0"
        if (!matchedParts || matchedParts.length < 3) {
            throw SfdxError.create('@salesforce/sfdx-scanner', 'jreSetupManager', 'VersionNotFound', []);
        }

        const versionPart1 = parseInt(matchedParts[1]);
        const versionPart2 = parseInt(matchedParts[2]);
        const version = versionPart1 + '.' + versionPart2;
        // We want to allow 1.8 and greater.
        // Upto JDK8, the version scheme is 1.blah
        // Starting JDK 9, the version scheme is 9.blah for 9, 10.blah for 10, etc.
        // If either version part clicks, we should be good.
        if (versionPart1 >= 1 || versionPart2 >= 8) {
            this.logger.trace(`Java version found as ${version}`);
            return;
        }

        // If we are here, version number is not what we are looking for
        throw SfdxError.create('@salesforce/sfdx-scanner', 'jreSetupManager', 'InvalidVersion', [version]);

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

    private async setJavaHomeInConfig(javaHome: string): Promise<void> {
        this.config.set(JAVA_HOME_KEY, javaHome);
        this.logger.trace(`Persisting config file with values ${this.config.values()}`);
        await this.config.write();
    }

    private async getJavaHomeFromConfig(): Promise<string> {
        const configExists = await this.config.exists();
        if (configExists) {
            const javaHomeJson = this.config.get(JAVA_HOME_KEY);
            this.logger.trace(`javaHomeJson read from config file: ${javaHomeJson}`);
            return javaHomeJson.valueOf() as string;
        }
        return null;
    }
}

// Method that'll be invoked externally
export async function verifyJreSetup(): Promise<string> {
    const manager = await JreSetupManager.create({});
    return await manager.verifyJreSetup();
}

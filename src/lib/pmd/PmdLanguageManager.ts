import {Config} from '../util/Config';
import {Controller} from '../../Controller';
import {LANGUAGE} from '../../Constants'
import {Logger, Messages, SfError} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import { ENGINE } from '../../Constants';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'PmdLanguageManager');

const VALID_LANGUAGES_BY_ALIAS: Map<string, string> = new Map([
	['apex', 'apex'],
	['java', 'java'],
	['javascript', 'javascript'],
	['ecmascript', 'javascript'],
	['js', 'javascript'],
	['vf', 'visualforce'],
	['visualforce', 'visualforce'],
	['xml', 'xml'],
	['pom', 'xml'],
	['xsl', 'xml']
]);

let INSTANCE: PmdLanguageManager = null;

class PmdLanguageManager extends AsyncCreatable {
	private logger: Logger;
	private config: Config;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('PmdLanguageManager');
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	public resolveLanguageAlias(alias: string): string {
		if (VALID_LANGUAGES_BY_ALIAS.has(alias.toLowerCase())) {
			const lang = VALID_LANGUAGES_BY_ALIAS.get(alias.toLowerCase());
			this.logger.trace(`Resolving language alias ${alias} to ${lang}`);
			return lang;
		} else {
			this.logger.trace(`No language found for alias ${alias}`);
			return null;
		}
	}

	public async getSupportedLanguages(): Promise<string[]> {
		const aliases = await this.config.getSupportedLanguages(ENGINE.PMD);
		const langs: string[] = [];
		for (const alias of aliases) {
			const lang = this.resolveLanguageAlias(alias);
			if (lang) {
				if (LANGUAGE.JAVASCRIPT === lang) {
					throw new SfError(messages.getMessage('JavascriptNotSupported'));
				} else {
					langs.push(lang);
				}
			} else {
				this.logger.trace(`Default-supported language alias ${alias} could not be resolved.`);
				throw new SfError(messages.getMessage('InvalidLanguageAlias', [this.config.getConfigFilePath(), alias]));
			}
		}
		return langs;
	}
}

export async function getSupportedLanguages(): Promise<string[]> {
	INSTANCE = INSTANCE || await PmdLanguageManager.create({});
	return await INSTANCE.getSupportedLanguages();
}

export async function resolveLanguageAlias(alias: string): Promise<string> {
	INSTANCE = INSTANCE || await PmdLanguageManager.create({});
	return INSTANCE.resolveLanguageAlias(alias);
}

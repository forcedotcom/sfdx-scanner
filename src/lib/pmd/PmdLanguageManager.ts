import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';
import {PmdEngine} from './PmdEngine';

import {Logger, SfdxError, Messages} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';

Messages.importMessagesDirectory(__dirname);
const LANGUAGES_BY_ALIAS: Map<string, string> = new Map([
	['apex', 'apex'],
	['java', 'java'],
	['javascript', 'javascript'],
	['ecmascript', 'javascript'],
	['js', 'javascript'],
	['jsp', 'jsp'],
	['modelica', 'modelica'],
	['plsql', 'plsql'],
	['pl/sql', 'plsql'],
	['pl-sql', 'plsql'],
	['scala', 'scala'],
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
		if (LANGUAGES_BY_ALIAS.has(alias.toLowerCase())) {
			const lang = LANGUAGES_BY_ALIAS.get(alias.toLowerCase());
			this.logger.trace(`Resolving language alias ${alias} to ${lang}`);
			return lang;
		} else {
			this.logger.trace(`No language found for alias ${alias}`);
			return null;
		}
	}

	public async getSupportedLanguages(): Promise<string[]> {
		const aliases = await this.config.getSupportedLanguages(PmdEngine.NAME);
		const langs: string[] = [];
		for (const alias of aliases) {
			const lang = this.resolveLanguageAlias(alias);
			if (lang) {
				langs.push(lang);
			} else {
				this.logger.trace(`Default-supported language alias ${alias} could not be resolved.`);
				throw SfdxError.create('@salesforce/sfdx-scanner', 'PmdLanguageManager', 'InvalidLanguageAlias', [alias]);
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

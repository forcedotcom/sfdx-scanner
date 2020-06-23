import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';
import {PmdEngine} from './PmdEngine';

import {Logger} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';

const DEFAULT_LANGUAGES = ['apex', 'javascript'];
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

	private resolveLanguageAliases(aliases: string[]): string[] {
		const resolvedLangs = [];
		for (let i = 0; i < aliases.length; i++) {
			const alias = aliases[i];
			if (LANGUAGES_BY_ALIAS.has(alias.toLowerCase())) {
				const lang = LANGUAGES_BY_ALIAS.get(alias.toLowerCase());
				this.logger.trace(`Resolving language alias ${alias} to ${lang}`);
				resolvedLangs.push(lang);
			} else {
				// TODO: Perhaps this should throw an error?
				this.logger.trace(`No language found for alias ${alias}`);
			}
		}
		return resolvedLangs;
	}

	public async getSupportedLanguages(): Promise<string[]> {
		const pmdConfig = await this.config.getEngineConfig(PmdEngine.NAME);
		// If the config specifies default languages, use those.
		if (pmdConfig.supportedLanguages && pmdConfig.supportedLanguages.length > 0) {
			this.logger.trace(`Pulled languages ${pmdConfig.supportedLanguages} from Config.json`);
			return this.resolveLanguageAliases(pmdConfig.supportedLanguages);
		} else {
			// Otherwise, assume they're using an old config that predates the property, and we'll set it to the default
			// value for them.
			pmdConfig.supportedLanguages = DEFAULT_LANGUAGES;
			await this.config.setEngineConfig(PmdEngine.NAME, pmdConfig);
			this.logger.trace(`Saving languages ${DEFAULT_LANGUAGES} to Config.json`);
			return DEFAULT_LANGUAGES;
		}
	}
}

export async function getSupportedLanguages(): Promise<string[]> {
	const manager = await PmdLanguageManager.create({});
	return await manager.getSupportedLanguages();
}

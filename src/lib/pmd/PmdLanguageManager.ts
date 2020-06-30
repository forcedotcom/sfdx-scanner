import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';


import {Logger, SfdxError, Messages} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import { ENGINE } from '../../Constants';

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
				this.logger.trace(`No language found for alias ${alias}`);
				throw SfdxError.create('@salesforce/sfdx-scanner', 'PmdLanguageManager', 'InvalidLanguageAlias', [alias]);
			}
		}
		return resolvedLangs;
	}

	public async getSupportedLanguages(): Promise<string[]> {
		const aliases = await this.config.getSupportedLanguages(ENGINE.PMD);
		return this.resolveLanguageAliases(aliases);
	}
}

export async function getSupportedLanguages(): Promise<string[]> {
	const manager = await PmdLanguageManager.create({});
	return await manager.getSupportedLanguages();
}

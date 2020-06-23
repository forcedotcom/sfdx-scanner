import {Config} from '../util/Config';
import {Controller} from '../../ioc.config';
import {PmdEngine} from './PmdEngine';

//import {Logger} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';

const DEFAULT_LANGUAGES = ['apex', 'javascript'];

class PmdLanguageManager extends AsyncCreatable {
	//private logger: Logger;
	private config: Config;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		//this.logger = await Logger.child('PmdLanguageManager');
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	public async getSupportedLanguages(): Promise<string[]> {
		const pmdConfig = await this.config.getEngineConfig(PmdEngine.NAME);
		// If the config specifies default languages, use those.
		if (pmdConfig.supportedLanguages && pmdConfig.supportedLanguages.length > 0) {
			return pmdConfig.supportedLanguages;
		} else {
			// Otherwise, assume they're using an old config that predates the property, and we'll set it to the default
			// value for them.
			pmdConfig.supportedLanguages = DEFAULT_LANGUAGES;
			await this.config.setEngineConfig(PmdEngine.NAME, pmdConfig);
			return DEFAULT_LANGUAGES;
		}
	}
}

export async function getSupportedLanguages(): Promise<string[]> {
	const manager = await PmdLanguageManager.create({});
	return await manager.getSupportedLanguages();
}

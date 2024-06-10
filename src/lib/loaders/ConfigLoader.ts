import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';

export interface ConfigLoader {
	loadConfig(configPath?: string): CodeAnalyzerConfig;
}

export class ConfigLoaderImpl implements ConfigLoader {
	public loadConfig(configPath?: string): CodeAnalyzerConfig {
		if (configPath) {
			return CodeAnalyzerConfig.fromFile(configPath);
		} else {
			return CodeAnalyzerConfig.withDefaults();
		}
	}
}

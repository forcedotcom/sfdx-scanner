import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';

export interface CodeAnalyzerConfigFactory {
	create(configPath?: string): CodeAnalyzerConfig;
}

export class CodeAnalyzerConfigFactoryImpl implements CodeAnalyzerConfigFactory {
	public create(configPath?: string): CodeAnalyzerConfig {
		if (configPath) {
			return CodeAnalyzerConfig.fromFile(configPath);
		} else {
			return CodeAnalyzerConfig.withDefaults();
		}
	}
}

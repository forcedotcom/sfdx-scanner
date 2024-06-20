import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';

export interface CodeAnalyzerConfigFactory {
	create(configPath?: string): CodeAnalyzerConfig;
}

export class CodeAnalyzerConfigFactoryImpl implements CodeAnalyzerConfigFactory {
	public create(configPath?: string): CodeAnalyzerConfig {
		if (configPath) {
			return CodeAnalyzerConfig.fromFile(configPath);

		// TODO: If the user did not provide a config-file then we should then check to see if the default one exists
		// at path.join(process.cwd(),'code-analyzer-config.yml') and if so then use that instead of using withDefaults.
		// This is important so that users don't always have to do --config-file <...> all the time.
		} else {
			return CodeAnalyzerConfig.withDefaults();
		}
	}
}

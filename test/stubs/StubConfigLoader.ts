import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {ConfigLoader} from '../../src/lib/loaders/ConfigLoader';

export class StubDefaultConfigLoader implements ConfigLoader {
	public loadConfig(_configPath?: string): CodeAnalyzerConfig {
		// Just return the default config. It's fine.
		return CodeAnalyzerConfig.withDefaults();
	}
}

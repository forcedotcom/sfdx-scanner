import * as path from 'node:path';
import * as fs from 'node:fs';
import {CONFIG_FILE_NAME, CONFIG_FILE_EXTENSIONS} from '../../Constants';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';

export interface CodeAnalyzerConfigFactory {
	create(configPath?: string): CodeAnalyzerConfig;
}

export class CodeAnalyzerConfigFactoryImpl implements CodeAnalyzerConfigFactory {

	public create(configPath?: string): CodeAnalyzerConfig {
		return this.getConfigFromProvidedPath(configPath)
			|| this.seekConfigInCurrentDirectory()
			|| CodeAnalyzerConfig.withDefaults();
	}

	private getConfigFromProvidedPath(configPath?: string): CodeAnalyzerConfig|undefined {
		return configPath ? CodeAnalyzerConfig.fromFile(configPath) : undefined;
	}

	private seekConfigInCurrentDirectory(): CodeAnalyzerConfig|undefined {
		for (const ext of CONFIG_FILE_EXTENSIONS) {
			const possibleConfigFilePath = path.resolve(`${CONFIG_FILE_NAME}.${ext}`);
			if (fs.existsSync(possibleConfigFilePath)) {
				return CodeAnalyzerConfig.fromFile(possibleConfigFilePath);
			}
		}
		return undefined;
	}
}

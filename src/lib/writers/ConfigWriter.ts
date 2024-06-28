import * as fs from 'node:fs';
import * as path from 'node:path';
import {CodeAnalyzerConfig} from '@salesforce/code-analyzer-core';
import {CONFIG_FILE_NAME, CONFIG_FILE_EXTENSIONS} from '../../Constants';

export interface ConfigWriter {
	write(config: CodeAnalyzerConfig): void;
}

export class ConfigFileWriter implements ConfigWriter {
	private readonly force: boolean;

	public constructor(force: boolean) {
		this.force = force;
	}

	public write(config: CodeAnalyzerConfig): void {
		const configFileName = `${CONFIG_FILE_NAME}.${CONFIG_FILE_EXTENSIONS[0]}`;
		const configFilePath = path.resolve('.', configFileName);
	}
}

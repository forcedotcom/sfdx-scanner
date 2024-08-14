import path from 'node:path';
import fs from 'node:fs';
import {ConfigModel, OutputFormat} from '../models/ConfigModel';
import {BundleName, getMessage} from '../messages';

export interface ConfigWriter {
	write(model: ConfigModel): void;
}

export class ConfigFileWriter implements ConfigWriter {
	private readonly file: string;
	private readonly format: OutputFormat;

	private constructor(file: string) {
		this.file = file;
		const ext = path.extname(file).toLowerCase();
		if (ext === '.yaml' || ext === '.yml') {
			this.format = OutputFormat.YAML;
		} else {
			throw new Error(getMessage(BundleName.ConfigWriter, 'error.unrecognized-file-format', [file]));
		}
	}

	public write(model: ConfigModel): void {
		fs.writeFileSync(this.file, model.toFormattedOutput(this.format));
	}

	public static fromFile(file: string): ConfigFileWriter {
		return new ConfigFileWriter(file);
	}
}

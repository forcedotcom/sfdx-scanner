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

	private constructor(file: string, format: OutputFormat) {
		this.file = file;
		this.format = format;
	}

	public write(model: ConfigModel): void {
		fs.writeFileSync(this.file, model.toFormattedOutput(this.format));
	}

	public static fromFile(file: string): ConfigFileWriter {
		const ext = path.extname(file).toLowerCase();
		if (ext === '.yaml' || ext === '.yml') {
			return new ConfigFileWriter(file, OutputFormat.RAW_YAML);
		} else {
			throw new Error(getMessage(BundleName.ConfigWriter, 'error.unrecognized-file-format', [file]));
		}
	}
}

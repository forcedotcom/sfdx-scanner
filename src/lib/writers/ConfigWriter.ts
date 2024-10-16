import path from 'node:path';
import fs from 'node:fs';
import {ConfigModel, OutputFormat} from '../models/ConfigModel';
import {BundleName, getMessage} from '../messages';
import {Display} from '../Display';
import {exists} from '../utils/FileUtil';

export interface ConfigWriter {
	write(model: ConfigModel): Promise<boolean>;
}

export class ConfigFileWriter implements ConfigWriter {
	private readonly file: string;
	private readonly format: OutputFormat;
	private readonly display: Display;

	private constructor(file: string, format: OutputFormat, display: Display) {
		this.file = file;
		this.format = format;
		this.display = display;
	}

	public async write(model: ConfigModel): Promise<boolean> {
		// Only write to the file if it doesn't already exist, or if the user confirms that they want to overwrite it.
		if (!(await exists(this.file)) || await this.display.confirm(getMessage(BundleName.ConfigWriter, 'prompt.overwrite-existing-file', [this.file]))) {
			fs.writeFileSync(this.file, model.toFormattedOutput(this.format));
			return true;
		} else {
			return false;
		}
	}

	public static fromFile(file: string, display: Display): ConfigFileWriter {
		const ext = path.extname(file).toLowerCase();
		if (ext === '.yaml' || ext === '.yml') {
			return new ConfigFileWriter(file, OutputFormat.RAW_YAML, display);
		} else {
			throw new Error(getMessage(BundleName.ConfigWriter, 'error.unrecognized-file-format', [file]));
		}
	}
}

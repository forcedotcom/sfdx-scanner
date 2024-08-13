

export interface ConfigWriter {
	write(): void;
}

export class ConfigFileWriter implements ConfigWriter {

	private constructor(_file: string) {
	}

	public write(): void {}

	public static fromFile(file: string): ConfigFileWriter {
		return new ConfigFileWriter(file);
	}
}

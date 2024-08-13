

export interface ConfigWriter {
	write(): void;
}

export class CompositeConfigWriter implements ConfigWriter {
	private readonly writers: ConfigWriter[] = [];

	private constructor(writers: ConfigWriter[]) {
		this.writers = writers;
	}

	public write(): void {
		this.writers.forEach(w => w.write());
	}

	public static fromFiles(files: string[]): CompositeConfigWriter {
		return new CompositeConfigWriter(files.map(f => new ConfigFileWriter(f)));
	}
}

class ConfigFileWriter implements ConfigWriter {

	public constructor(_file: string) {
	}

	public write(): void {}
}

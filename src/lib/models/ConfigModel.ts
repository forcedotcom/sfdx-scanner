
export enum OutputFormat {
	YAML = "YAML"
}

export interface ConfigModel {
	toFormattedOutput(format: OutputFormat): string;
}

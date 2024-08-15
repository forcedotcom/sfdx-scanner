import {Display} from '../Display';
import {ConfigModel, OutputFormat} from '../models/ConfigModel';


export interface ConfigViewer {
	view(configModel: ConfigModel): void;
}

export class ConfigRawYamlViewer implements ConfigViewer {
	private readonly display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(configModel: ConfigModel): void {
		this.display.displayLog(configModel.toFormattedOutput(OutputFormat.YAML));
	}
}

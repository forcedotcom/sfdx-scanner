import {Display} from '../Display';
import {ConfigModel, OutputFormat} from '../models/ConfigModel';
import {makeGrey} from '../utils/StylingUtil';


export interface ConfigViewer {
	view(configModel: ConfigModel): void;
}

export class ConfigStyledYamlViewer implements ConfigViewer {
	private readonly display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(configModel: ConfigModel): void {
		const styledYaml = configModel.toFormattedOutput(OutputFormat.YAML)
			// We're seeking `#`, the YAML comment character, and greying out that character and the rest of the line.
			// Known Bug: We don't distinguish between `#` characters inside and outside of strings or the like. In the
			//            event that a `#` is part of a string, it will still be perceived by this line as part of a
			//            comment and formatted as such. For now, we're accepting this, because it's an unlikely scenario
			//            to begin with, and because it's purely a cosmetic UI issue.
			.replaceAll(/#.+/gm, s => makeGrey(s));
		// Prepend a newline to visually separate the output from anything else we've already logged.
		this.display.displayLog('\n' + styledYaml);
	}
}

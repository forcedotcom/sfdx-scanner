import {Display} from '../Display';


export interface ConfigViewer {
	view(): void;
}

export class ConfigDisplayViewer implements ConfigViewer {

	public constructor(_display: Display) {

	}

	public view(): void {

	}
}

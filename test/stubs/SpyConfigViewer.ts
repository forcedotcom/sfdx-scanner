import {ConfigModel} from '../../src/lib/models/ConfigModel';
import {ConfigViewer} from '../../src/lib/viewers/ConfigViewer';

export class SpyConfigViewer implements ConfigViewer {
	private callHistory: ConfigModel[] = [];

	public view(config: ConfigModel): void {
		this.callHistory.push(config);
	}

	public getCallHistory(): ConfigModel[] {
		return this.callHistory;
	}
}

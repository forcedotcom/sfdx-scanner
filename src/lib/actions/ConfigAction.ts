import {ConfigWriter} from '../writers/ConfigWriter';
import {ConfigViewer} from '../viewers/ConfigViewer';
import {LogEventListener} from '../listeners/LogEventListener';
import {ConfigModel, DummyConfigModel} from '../models/ConfigModel';

export type ConfigDependencies = {
	logEventListeners: LogEventListener[];
	writer?: ConfigWriter;
	viewer: ConfigViewer;
};

export type ConfigInput = {
	'config-file'?: string;
	'rule-selector': string[];
	workspace?: string[];
};

export class ConfigAction {
	private readonly dependencies: ConfigDependencies;

	private constructor(dependencies: ConfigDependencies) {
		this.dependencies = dependencies;

	}

	public execute(_input: ConfigInput): Promise<void> {
		// TODO: Use the input to construct a more intelligent Config Model; possibly depend on a ConfigModelGenerator?
		const configModel: ConfigModel = new DummyConfigModel();

		this.dependencies.viewer.view(configModel);
		this.dependencies.writer?.write(configModel);
		return Promise.resolve();
	}

	public static createAction(dependencies: ConfigDependencies): ConfigAction {
		return new ConfigAction(dependencies)
	}
}

import {ConfigWriter} from '../writers/ConfigWriter';
import {ConfigViewer} from '../viewers/ConfigViewer';
import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {LogEventListener} from '../listeners/LogEventListener';

export type ConfigDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
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

	private constructor(_dependencies: ConfigDependencies) {

	}

	public async execute(_input: ConfigInput): Promise<void> {

	}

	public static createAction(dependencies: ConfigDependencies): ConfigAction {
		return new ConfigAction(dependencies)
	}
}

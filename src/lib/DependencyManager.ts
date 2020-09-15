import {OUTPUT_FORMAT} from '../Constants';


export interface DependencyManager {
	init(): Promise<void>;

	scanForInsecureDependencies(targets: string[], format: OUTPUT_FORMAT): Promise<any>;
}

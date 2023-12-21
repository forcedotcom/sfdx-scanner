import {Inputs} from "../types";
import normalize = require('normalize-path');
import path = require('path');
import untildify = require("untildify");

export interface PathFactory {
	createTargetPaths(inputs: Inputs): string[];

	createProjectDirPaths(inputs: Inputs): string[];
}

export class PathFactoryImpl implements PathFactory{
	// TODO: Stop allowing an array of paths - move towards only 1 path
	createProjectDirPaths(inputs: Inputs): string[] {
		if (inputs.projectdir && (inputs.projectdir as string[]).length > 0) {
			return (inputs.projectdir as string[]).map(p => path.resolve(p));
		}
		return [];
	}

	createTargetPaths(inputs: Inputs): string[] {
		// Turn the paths into normalized Unix-formatted paths and strip out any single- or double-quotes, because
		// sometimes shells are stupid and will leave them in there.
		const target: string[] = (inputs.target || []) as string[];
		return target.map(path => normalize(untildify(path)).replace(/['"]/g, ''));
	}

}

import {Inputs} from "../types";
import normalize = require('normalize-path');
import path = require('path');
import untildify = require("untildify");

export interface PathResolver {
	resolvePaths(inputs: Inputs): string[];

	resolveTargetPaths(inputs: Inputs): string[];

	resolveProjectDirPaths(inputs: Inputs): string[];
}

export class PathResolverImpl implements PathResolver{
	public resolvePaths(inputs: Inputs): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return (inputs.path as string[]).map(p => path.resolve(untildify(p)));
	}

	public resolveProjectDirPaths(inputs: Inputs): string[] {
		// TODO: Stop allowing an array of paths - move towards only 1 path (to resolve into 1 output path)
		if (inputs.projectdir && (inputs.projectdir as string[]).length > 0) {
			return (inputs.projectdir as string[]).map(p => path.resolve(p));
		}
		return [];
	}

	public resolveTargetPaths(inputs: Inputs): string[] {
		// Turn the paths into normalized Unix-formatted paths and strip out any single- or double-quotes, because
		// sometimes shells are stupid and will leave them in there.
		const target: string[] = (inputs.target || []) as string[];
		return target.map(path => normalize(untildify(path)).replace(/['"]/g, ''));
	}

}

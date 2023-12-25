import {Inputs} from "../types";
import normalize = require('normalize-path');
import path = require('path');
import untildify = require("untildify");
import {RunOptions} from "./RuleManager";
import {RunOutputOptions} from "./util/RunOutputProcessor";
import {inferFormatFromOutfile, OutputFormat} from "./output/OutputFormat";

/**
 * Service for processing inputs
 */
export interface InputProcessor {
	resolvePaths(inputs: Inputs): string[];

	resolveTargetPaths(inputs: Inputs): string[];

	resolveProjectDirPaths(inputs: Inputs): string[];

	createRunOptions(inputs: Inputs, isDfa: boolean): RunOptions;

	createRunOutputOptions(inputs: Inputs): RunOutputOptions;
}

export class InputProcessorImpl implements InputProcessor {
	private readonly sfVersion: string;

	public constructor(sfVersion: string) {
		this.sfVersion = sfVersion;
	}

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


	public createRunOptions(inputs: Inputs, isDfa: boolean): RunOptions {
		return {
			normalizeSeverity: (inputs['normalize-severity'] || inputs['severity-threshold']) as boolean,
			runDfa: isDfa,
			withPilot: inputs['with-pilot'] as boolean,
			sfVersion: this.sfVersion
		};
	}

	public createRunOutputOptions(inputs: Inputs): RunOutputOptions {
		return {
			format: outputFormatFromInputs(inputs),
			severityForError: inputs['severity-threshold'] as number,
			outfile: inputs.outfile as string
		};
	}
}

function outputFormatFromInputs(inputs: Inputs): OutputFormat {
	if (inputs.format) {
		return inputs.format as OutputFormat;
	} else if (inputs.outfile) {
		return inferFormatFromOutfile(inputs.outfile);
	} else if (inputs.json) {
		return OutputFormat.JSON;
	} else {
		return OutputFormat.TABLE;
	}
}

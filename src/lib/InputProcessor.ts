import {Inputs} from "../types";
import {RunOptions} from "./RuleManager";
import {RunOutputOptions} from "./output/RunResultsProcessor";
import {inferFormatFromOutfile, OutputFormat} from "./output/OutputFormat";
import {SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../MessageCatalog";
import {INTERNAL_ERROR_CODE} from "../Constants";
import {Display} from "./Display";
import normalize = require('normalize-path');
import path = require('path');
import fs = require('fs');
import untildify = require("untildify");
import globby = require('globby');
import {Tokens} from "@salesforce/core/lib/messages";

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
	private readonly display: Display;
	private readonly displayedKeys: Set<string>;

	public constructor(sfVersion: string, display: Display) {
		this.sfVersion = sfVersion;
		this.display = display
		this.displayedKeys = new Set();
	}

	public resolvePaths(inputs: Inputs): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return (inputs.path as string[]).map(p => path.resolve(untildify(p)));
	}

	public resolveProjectDirPaths(inputs: Inputs): string[] {
		// If projectdir is provided, then return it since at this point it has already been validated to exist
		if (inputs.projectdir && (inputs.projectdir as string[]).length > 0) {
			return (inputs.projectdir as string[]).map(p => path.resolve(normalize(untildify(p))))
		}

		// If projectdir is not provided then:
		// * We calculate the first common parent directory that includes all the target files.
		// --> If none of its parent folders contain a sfdx-project.json file, then we return this first common parent.
		// --> Otherwise we return the folder that contains the sfdx-project.json file.
		const commonParentFolder = getFirstCommonParentFolder(this.getAllTargetFiles(inputs));
		let projectFolder: string = findFolderThatContainsSfdxProjectFile(commonParentFolder);
		projectFolder = projectFolder.length > 0 ? projectFolder : commonParentFolder
		this.displayInfoOnlyOnce('info.resolvedProjectDir', [projectFolder])
		return [projectFolder];
	}

	public resolveTargetPaths(inputs: Inputs): string[] {
		// Turn the paths into normalized Unix-formatted paths and strip out any single- or double-quotes, because
		// sometimes shells are stupid and will leave them in there.
		// Note that we do not do a path.resolve since the target input can be globby (which we handle elsewhere).

		// If possible, in the future we should resolve all globs here instead of in the DefaultRuleManager.
		// Also, I would recommend that we eventually resolve globs based on the projectdir (since it acts as a
		// root directory) instead of the present working directory.
		if (!inputs.target) {
			this.displayInfoOnlyOnce('info.resolvedTarget')
		}
		const targetPaths: string[] = (inputs.target || ['.']) as string[];
		return targetPaths.map(path => normalize(untildify(path)).replace(/['"]/g, ''));
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
			verboseViolations: inputs['verbose-violations'] as boolean,
			severityForError: inputs['severity-threshold'] as number,
			outfile: inputs.outfile as string
		};
	}

	private getAllTargetFiles(inputs: Inputs): string[] {
		const targetPaths: string[] = this.resolveTargetPaths(inputs).map(p => trimMethodSpecifier(p))
		const allAbsoluteTargetFiles: string[] = globby.sync(targetPaths).map(p => path.resolve(p));
		if (allAbsoluteTargetFiles.length == 0) {
			throw new SfError(getMessage(BundleName.CommonRun, 'validations.noFilesFoundInTarget'), null, null, INTERNAL_ERROR_CODE);
		}
		return allAbsoluteTargetFiles;
	}

	private displayInfoOnlyOnce(messageKey: string, tokens?: Tokens) {
		if (!this.displayedKeys.has(messageKey)) {
			this.display.displayInfo(getMessage(BundleName.CommonRun, messageKey, tokens));
			this.displayedKeys.add(messageKey);
		}
	}
}

function outputFormatFromInputs(inputs: Inputs): OutputFormat {
	if (inputs.format) {
		return inputs.format as OutputFormat;
	} else if (inputs.outfile) {
		return inferFormatFromOutfile(inputs.outfile as string);
	} else if (inputs.json) {
		return OutputFormat.JSON;
	} else {
		return OutputFormat.TABLE;
	}
}

function trimMethodSpecifier(targetPath: string): string {
	const lastHashPos: number = targetPath.lastIndexOf('#');
	return lastHashPos < 0 ? targetPath : targetPath.substring(0, lastHashPos)
}

function getFirstCommonParentFolder(targetFiles: string[]) {
	const longestCommonStr: string = getLongestCommonPrefix(targetFiles);
	const commonParentFolder = getParentFolderOf(longestCommonStr);
	return commonParentFolder.length == 0 ? path.sep : commonParentFolder;
}

function getLongestCommonPrefix(strs: string[]): string {
	// To find the longest common prefix, we first get the select the shortest string from our list of strings
	const shortestStr = strs.reduce((s1, s2) => s1.length <= s2.length ? s1 : s2);

	// Then we check that each string's ith character is the same as the shortest strings ith character
	for (let i = 0; i < shortestStr.length; i++) {
		if(!strs.every(str => str[i] === shortestStr[i])) {
			// If we find a string that doesn't match the ith character, we return the common prefix from [0,i)
			return shortestStr.substring(0, i)
		}
	}
	return shortestStr;
}

function findFolderThatContainsSfdxProjectFile(folder: string): string {
	let folderToCheck: string = folder;
	while (folderToCheck.length > 0) {
		if (fs.existsSync(path.resolve(folderToCheck, 'sfdx-project.json'))) {
			return folderToCheck;
		}
		folderToCheck = getParentFolderOf(folderToCheck);
	}
	return '';
}

function getParentFolderOf(fileOrFolder: string): string {
	return fileOrFolder.substring(0, fileOrFolder.lastIndexOf(path.sep))
}

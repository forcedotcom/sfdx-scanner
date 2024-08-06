import path from 'node:path';
import * as fg from 'fast-glob';
import {CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';

export async function createWorkspace(core: CodeAnalyzer, workspacePaths: string[]): Promise<Workspace> {
	const processedPaths: string[] = [];
	for (const workspacePath of workspacePaths) {
		// Globs assume the path separator character is '/' and treat '\\' as an escape character. So on OSs where
		// the path separator character isn't '/', we need to convert paths into Glob syntax before checking whether
		// they're actually Globs, to prevent misidentification.
		const normalizedWorkspacePath: string = path.sep === '/' ? workspacePath : fg.convertPathToPattern(workspacePath);
		if (fg.isDynamicPattern(normalizedWorkspacePath)) {
			// NOTE: We're pushing the strict results of the Glob, meaning that the paths will be normalized to UNIX style.
			processedPaths.push(...await fg.glob(normalizedWorkspacePath));
		} else {
			processedPaths.push(workspacePath);
		}
	}
	// Part of the contract for Core's `createWorkspace()` method is that paths are localized to the current OS, hence
	// it's not a problem for Glob results to be UNIX-formatted since they'll be converted right back.
	return core.createWorkspace(processedPaths);
}

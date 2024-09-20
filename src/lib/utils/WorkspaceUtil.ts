import * as fg from 'fast-glob';
import {CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';
import {getMessage, BundleName} from '../messages';

export async function createWorkspace(core: CodeAnalyzer, workspacePaths: string[]): Promise<Workspace> {
	const processedPaths: string[] = [];
	for (const workspacePath of workspacePaths) {
		// Negative globs are not currently supported.
		if (workspacePath.includes("!")) {
			throw new Error(getMessage(BundleName.WorkspaceUtil, 'error.negative-globs-unsupported', [workspacePath]));
		// If the path has a star (*) or question mark in it, we assume it's a glob.
		} else if (/[*?]/.test(workspacePath)) {
			// For the convenience of Windows users, we'll normalize glob paths into UNIX style, so they're valid globs.
			const normalizedWorkspacePath = workspacePath.replace(/[\\/]/g, '/');
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
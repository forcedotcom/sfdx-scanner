import path from 'node:path';
import * as fg from 'fast-glob';
import {CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';

export class WorkspaceFactory {

	public async create(core: CodeAnalyzer, workspacePaths: string[]): Promise<Workspace> {
		const processedPaths: string[] = [];
		for (const workspacePath of workspacePaths) {
			// Globs assume the path separator character is '/' and treat '\\' as an escape character. So on OSs where
			// the path separator character isn't '/', we need to convert paths into Glob syntax before checking whether
			// they're actually Globs, to prevent misidentification.
			const normalizedWorkspacePath: string = path.sep === '/' ? workspacePath : fg.convertPathToPattern(workspacePath);
			if (fg.isDynamicPattern(normalizedWorkspacePath)) {
				// NOTE: We are returning the strict results of the Glob here, meaning that the paths will be normalized
				// to UNIX style. If this is discovered to cause problems downstream on Windows, then we'll need to
				// identify a fix.
				processedPaths.push(...await fg.glob(normalizedWorkspacePath));
			} else {
				processedPaths.push(workspacePath);
			}
		}
		return core.createWorkspace(processedPaths);
	}
}

import globby from 'globby';
import normalizePath from 'normalize-path';
import {CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';

export class WorkspaceFactory {

	public async create(core: CodeAnalyzer, workspacePaths: string[]): Promise<Workspace> {
		const processedPaths: string[] = [];
		for (const workspacePath of workspacePaths) {
			// We need to normalize the path here, because otherwise ordinary Windows paths will be misidentified as Globs
			// due to the path separator (\) also being the Glob syntax escape character.
			const normalizedWorkspacePath: string = normalizePath(workspacePath);
			if (globby.hasMagic(normalizedWorkspacePath)) {
				// NOTE: We are returning the strict results of the Glob here, meaning that the paths will be normalized
				// to UNIX style. If this is discovered to cause problems downstream on Windows, then we'll need to
				// identify a fix.
				processedPaths.push(...(await globby(normalizedWorkspacePath)));
			} else {
				processedPaths.push(workspacePath);
			}
		}
		return core.createWorkspace(processedPaths);
	}
}

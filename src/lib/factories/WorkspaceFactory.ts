import globby from 'globby';
import {CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';

export class WorkspaceFactory {

	public async create(core: CodeAnalyzer, workspacePaths: string[]): Promise<Workspace> {
		const processedPaths: string[] = [];
		for (const workspacePath of workspacePaths) {
			if (globby.hasMagic(workspacePath)) {
				processedPaths.push(...(await globby(workspacePath)));
			} else {
				processedPaths.push(workspacePath);
			}
		}
		return core.createWorkspace(processedPaths);
	}
}

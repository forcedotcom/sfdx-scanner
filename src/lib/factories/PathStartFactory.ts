import globby from 'globby';
import path from 'node:path';
import {getMessage, BundleName} from '../messages';

export class PathStartFactory {
	public async create(pathStarts?: string[]): Promise<string[]|undefined> {
		if (pathStarts == null) {
			return pathStarts;
		}
		const processedPaths: string[] = [];
		for (const pathStart of pathStarts) {
			if (globby.hasMagic(pathStart)) {
				// Globs and method-level targeting are mutually exclusive.
				if (path.basename(pathStart).includes('#')) {
					throw new Error(getMessage(BundleName.PathStartFactory, 'error.glob-method-conflict', [pathStart]));
				}
				processedPaths.push(...(await globby(pathStart)));
			} else {
				processedPaths.push(pathStart);
			}
		}
		return processedPaths;
	}
}

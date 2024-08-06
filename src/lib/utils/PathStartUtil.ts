import path from 'node:path';
import * as fg from 'fast-glob';
import {getMessage, BundleName} from '../messages';

export async function createPathStarts(pathStarts?: string[]): Promise<string[]|undefined> {
	if (pathStarts == null) {
		return pathStarts;
	}
	const processedPaths: string[] = [];
	for (const pathStart of pathStarts) {
		// Globs assume the path separator character is '/' and treat '\\' as an escape character. So on OSs where
		// the path separator character isn't '/', we need to convert paths into Glob syntax before checking whether
		// they're actually Globs, to prevent misidentification.
		const normalizedPathStart: string = path.sep === '/' ? pathStart: fg.convertPathToPattern(pathStart);
		if (fg.isDynamicPattern(normalizedPathStart)) {
			// Globs and method-level targeting are mutually exclusive.
			if (path.basename(pathStart).includes('#')) {
				throw new Error(getMessage(BundleName.PathStartUtil, 'error.glob-method-conflict', [pathStart]));
			}
			// Since Glob results are UNIX-styled, we need to convert the results to use whatever the local path separator
			// character is.
			processedPaths.push(...(await fg.glob(normalizedPathStart)).map(p => p.replace(/[\\/]/g, path.sep)));
		} else {
			processedPaths.push(pathStart);
		}
	}
	return processedPaths;
}

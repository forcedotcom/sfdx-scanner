import path from 'node:path';
import * as fg from 'fast-glob';
import {getMessage, BundleName} from '../messages';

export async function createPathStarts(pathStarts?: string[]): Promise<string[]|undefined> {
	if (pathStarts == null) {
		return pathStarts;
	}
	const processedPaths: string[] = [];
	for (const pathStart of pathStarts) {
		// Negative globs are not currently supported.
		if (pathStart.includes("!")) {
			throw new Error(getMessage(BundleName.PathStartUtil, 'error.negative-globs-unsupported', [pathStart]));
		// If the path a star (*) or question mark in it, we assume it's a glob.
		} else if (/[*?]/.test(pathStart)) {
			// For the convenience of Windows users, we'll normalize glob paths into UNIX style, so they're valid globs.
			const normalizedPathStart = pathStart.replace(/[\\/]/g, '/');
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

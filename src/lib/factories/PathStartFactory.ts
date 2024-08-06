import path from 'node:path';
import * as fg from 'fast-glob';
import {getMessage, BundleName} from '../messages';

export class PathStartFactory {
	public async create(pathStarts?: string[]): Promise<string[]|undefined> {
		if (pathStarts == null) {
			return pathStarts;
		}
		const processedPaths: string[] = [];
		for (const pathStart of pathStarts) {
			// Globs assume the path separator character is '/' and treat '\\' as an escape character. So on OSs where
			// the path separator character isn't '/', we need to convert paths into Glob syntax before checking whether
			// they're actually Globs, to prevent misidentification.
			const normalizedPathStart: string = path.sep === '/' ? pathStart : fg.convertPathToPattern(pathStart);
			if (fg.isDynamicPattern(normalizedPathStart)) {
				// Globs and method-level targeting are mutually exclusive.
				if (path.basename(pathStart).includes('#')) {
					throw new Error(getMessage(BundleName.PathStartFactory, 'error.glob-method-conflict', [pathStart]));
				}
				// NOTE: We are returning the strict results of the Glob here, meaning that the paths will be normalized
				// to UNIX style. If this is discovered to cause problems downstream on Windows, then we'll need to
				// identify a fix.
				processedPaths.push(...await fg.glob(normalizedPathStart));
			} else {
				processedPaths.push(pathStart);
			}
		}
		return processedPaths;
	}
}

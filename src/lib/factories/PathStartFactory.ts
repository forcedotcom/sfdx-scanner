import globby from 'globby';
import normalizePath from 'normalize-path';
import path from 'node:path';
import {getMessage, BundleName} from '../messages';

export class PathStartFactory {
	public async create(pathStarts?: string[]): Promise<string[]|undefined> {
		if (pathStarts == null) {
			return pathStarts;
		}
		const processedPaths: string[] = [];
		for (const pathStart of pathStarts) {
			// We need to normalize the path here, because otherwise ordinary Windows paths will be misidentified as Globs
			// due to the path separator (\) also being the Glob syntax escape character.
			const normalizedPathStart: string = normalizePath(pathStart);
			if (globby.hasMagic(normalizedPathStart)) {
				// Globs and method-level targeting are mutually exclusive.
				if (path.basename(pathStart).includes('#')) {
					throw new Error(getMessage(BundleName.PathStartFactory, 'error.glob-method-conflict', [pathStart]));
				}
				// NOTE: We are returning the strict results of the Glob here, meaning that the paths will be normalized
				// to UNIX style. If this is discovered to cause problems downstream on Windows, then we'll need to
				// identify a fix.
				processedPaths.push(...(await globby(normalizedPathStart)));
			} else {
				processedPaths.push(pathStart);
			}
		}
		return processedPaths;
	}
}

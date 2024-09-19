import * as fs from 'node:fs';

export function exists(filename: string): boolean {
	try {
		fs.accessSync(filename, fs.constants.F_OK);
		return true;
	} catch (e) {
		return false;
	}
}

import * as fs from 'node:fs';

export async function exists(filename: string): Promise<boolean> {
	try {
		await fs.promises.access(filename, fs.constants.F_OK);
		return true;
	} catch (_e) {
		return false;
	}
}

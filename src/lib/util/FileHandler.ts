import {Stats, promises as fs, constants as fsConstants} from 'fs';
import tmp = require('tmp');

type DuplicationFn = (src: string, target: string) => Promise<void>;

/**
 * Handles all File and IO operations.
 * Mock this class to override file change behavior from unit tests.
 */
export class FileHandler {
	async exists(filename: string): Promise<boolean> {
		try {
			await fs.access(filename, fsConstants.F_OK);
			return true;
		} catch (e) {
			return false;
		}
	}

	stats(filename: string): Promise<Stats> {
		return fs.stat(filename);
	}

	async isDir(filename: string): Promise<boolean> {
		return await this.exists(filename) && (await this.stats(filename)).isDirectory();
	}

	async isFile(filename: string): Promise<boolean> {
		return await this.exists(filename) && (await this.stats(filename)).isFile();
	}

	readDir(filename: string): Promise<string[]> {
		return fs.readdir(filename);
	}

	readFileAsBuffer(filename: string): Promise<Buffer> {
		return fs.readFile(filename);
	}

	readFile(filename: string): Promise<string> {
		return fs.readFile(filename, 'utf-8');
	}

	async mkdirIfNotExists(dir: string): Promise<void> {
		await fs.mkdir(dir, {recursive: true});
		return;
	}

	writeFile(filename: string, fileContent: string): Promise<void> {
		return fs.writeFile(filename, fileContent);
	}

	// Create a temp file that will automatically be cleaned up when the process exits.
	// Returns the absolute path of the temp file
	tmpFileWithCleanup(): Promise<string> {
		return new Promise<string>((resolve, reject) => {
			// Ask tmp to clean up the file on process exit
			tmp.setGracefulCleanup();
			return tmp.file({}, (err, name) => {
				if (!err) {
					resolve(name);
				} else {
					reject(err);
				}
			});
		});
	}

	tmpDirWithCleanup(): Promise<string> {
		return new Promise<string>((res, rej) => {
			// Ask tmp to gracefully clean up everything on process exit.
			tmp.setGracefulCleanup();
			return tmp.dir({unsafeCleanup: true}, (err, name) => {
				if (!err) {
					res(name);
				} else {
					rej(err);
				}
			});
		});
	}

	async duplicateFile(src: string, target: string): Promise<void> {
		// NOTE: This method is likely to be called many times concurrently, and it's probably possible to optimize it a
		// bit so we don't try duplication methods we know will fail. However, that introduces risk that we'd rather avoid
		// at the moment. So we'll go with this semi-naive implementation, aware that it performs SLIGHTLY worse than
		// an optimal one, and prepared to address it if there's somehow a problem.
		// These are the file duplication functions available to us, in order of preference.
		const dupFns: DuplicationFn[] = [fs.symlink, fs.link, fs.copyFile];
		const errMsgs: string[] = [];

		// Iterate over the potential duplication methods....
		for (const dupFn of dupFns) {
			try {
				// For each one, try applying it...
				await dupFn(src, target);
				// ... and if it worked, then we're done.
				return;
			} catch (e) {
				// Meanwhile, if it failed, add its error message to our message list.
				const message: string = e instanceof Error ? e.message : e as string;
				errMsgs.push(`${dupFn.name}: ${message}`);
			}
		}
		// If we're here, then we're out of duplication functions to try. We'll need to combine the error messages we've
		// built up in order to make something informative and helpful.
		throw new Error(`All attempts to duplicate file ${src} have failed.\n${errMsgs.join('\n')}`);
	}
}

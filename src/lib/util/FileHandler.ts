import fs = require('fs');
import {Stats} from 'fs';
import tmp = require('tmp');

type DuplicationFn = (src: string, target: string, callback: (err: Error) => void) => void;

/**
 * Handles all File and IO operations.
 * Mock this class to override file change behavior from unit tests.
 */
export class FileHandler {
	exists(filename: string): Promise<boolean> {
		return new Promise<boolean>((resolve) => {
			fs.access(filename, fs.constants.F_OK, (err) => {
				resolve(!err);
			});
		});
	}

	stats(filename: string): Promise<Stats> {
		return new Promise<Stats>((resolve, reject) => {
			return fs.stat(filename, ((err, stats) => {
				if(!err) {
					resolve(stats);
				} else {
					reject(err);
				}
			}));
		});
	}

	async isDir(filename: string): Promise<boolean> {
		return await this.exists(filename) && (await this.stats(filename)).isDirectory();
	}

	readDir(filename: string): Promise<string[]> {
		return new Promise<string[]>((resolve, reject) => {
			return fs.readdir(filename, ((err, files) => {
				if(!err) {
					resolve(files);
				} else {
					reject(err);
				}
			}));
		});
	}

	readFile(filename: string): Promise<string> {
		return new Promise<string>((resolve, reject) => {
			return fs.readFile(filename, 'utf-8', ((err, data) => {
				if(!err) {
					resolve(data);
				} else {
					reject(err);
				}
			}));
		});
	}

	mkdirIfNotExists(dir: string): Promise<void> {
		return new Promise<void>((resolve, reject) => {
			return fs.mkdir(dir, {recursive: true}, (err) => {
				if(!err) {
					resolve();
				} else {
					reject(err);
				}
			});
		});
	}

	writeFile(filename: string, fileContent: string): Promise<void> {
		return new Promise<void>((resolve, reject) => {
			return fs.writeFile(filename, fileContent, (err) => {
				if(!err) {
					resolve();
				} else {
					reject(err);
				}
			});
		});
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

	duplicateFile(src: string, target: string): Promise<void> {
		// NOTE: This method is likely to be called many times concurrently, and it's probably possible to optimize it a
		// bit so we don't try duplication methods we know will fail. However, that introduces risk that we'd rather avoid
		// at the moment. So we'll go with this semi-naive implementation, aware that it performs SLIGHTLY worse than
		// an optimal one, and prepared to address it if there's somehow a problem.
		// These are the file duplication functions available to us, in order of preference.
		const dupFns: DuplicationFn[] = [fs.symlink, fs.link, fs.copyFile];
		const errs: Error[] = [];

		return new Promise<void>((res, rej) => {
			const dfIterator: IterableIterator<DuplicationFn> = dupFns.values();

			function iterativelyAttemptDuplication(): void {
				const {value, done} = dfIterator.next();
				if (done) {
					// If `done` is true, it means we're out of duplication functions to try. We'll need to combine the
					// error messages we got in order to make something informative and helpful.
					const errMsgs: string[] = [];
					for (let i = 0; i < errs.length; i++) {
						errMsgs.push(`${dupFns[i].name}: ${errs[i].message || errs[i]}`);
					}
					rej(`All attempts to duplicate file ${src} failed.\n${errMsgs.join('\n')}`);
				} else {
					// If `done` is false, then we have another duplication function we can try.
					value(src, target, (err) => {
						// Handle errors by adding them to the array and making a recursive call to try again.
						if (err) {
							errs.push(err);
							iterativelyAttemptDuplication();
						} else {
							// If there was no error, then the duplication succeeded, and we're in the clear.
							res();
						}
					});
				}
			}

			// Start our recursion.
			iterativelyAttemptDuplication();
		});
	}
}

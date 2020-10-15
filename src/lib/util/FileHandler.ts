import fs = require('fs');
import {Stats} from 'fs';
import tmp = require('tmp');

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

	symlinkFile(src: string, target: string): Promise<void> {
		return new Promise<void>((res, rej) => {
			fs.symlink(src, target, (err) => {
				if (err) {
					rej(err);
				} else {
					res();
				}
			});
		});
	}
}

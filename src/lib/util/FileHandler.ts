import fs = require('fs');
import {Stats} from 'fs';

/**
 * Handles all File and IO operations.
 * Mock this class to override file change behavior from unit tests.
 */
export class FileHandler {

	stats(filename: string): Promise<Stats> {
		return fs.promises.stat(filename);
	}

	readDir(filename: string): Promise<string[]> {
		return fs.promises.readdir(filename);
	}

	readFile(filename: string): Promise<string> {
		return fs.promises.readFile(filename, 'utf-8');
	}

	mkdirIfNotExists(dir: string): Promise<string> {
		return fs.promises.mkdir(dir, {recursive: true});
	}

	writeFile(filename: string, fileContent: string): Promise<void> {
		return fs.promises.writeFile(filename, fileContent);
	}
}

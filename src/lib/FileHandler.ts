import fs = require('fs');

/**
 * Handles all File and IO operations. 
 * Mock this class to override file change behavior from unit tests.
 */
export class FileHandler {

    async readFile(filename: string): Promise<string> {
        return await fs.promises.readFile(filename, 'utf-8');
    }

    async mkdirIfNotExists(dir: string): Promise<void> {
        await fs.promises.mkdir(dir, { recursive: true });
    }

    async writeFile(filename: string, fileContent: string): Promise<void> {
        await fs.promises.writeFile(filename, fileContent);
    }
}

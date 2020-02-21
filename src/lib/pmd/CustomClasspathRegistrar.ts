import fs = require('fs');
import path = require('path');
import util = require('util');

export const CUSTOM_CLASSPATH_REGISTER = path.join('catalogs', '.CustomPaths.json');

/**
 * Handles registering classpaths for custom rules into CustomPaths JSON
 * 
 * TODO: verify validity of path. Better to fail now than later while creating catalog.
 */

export class CustomClasspathRegistrar {

    public async createEntries(language: string, location: string[]) {

        // Fetch current entries in custom rule register as a Map
        const currentEntries = await this.readCurrentEntries();

        // If current language has entries, append new paths to existing entry
        if (currentEntries.has(language)) {
            const currentValue = currentEntries.get(language);
            location.forEach((item) => {
                currentValue.push(item);
            });

        } else {
            // When current language does not exist, create a new entry
            currentEntries.set(language, location);
        }
        
        // Write updated Map to file
        await this.writeToJson(this.mapToJson(currentEntries));

    }

    async readCurrentEntries(): Promise<Map<string, string[]>> {
        let mapFromFile = new Map<string, string[]>();

        const readFilePromise = util.promisify(fs.readFile);
        
        try {
            const data = await readFilePromise(CUSTOM_CLASSPATH_REGISTER, "utf-8");
            const json = JSON.parse(data);
            mapFromFile = this.jsonToMap(json);
        } catch (err) {
            if (err.code = 'ENOENT') {
                console.log(`${CUSTOM_CLASSPATH_REGISTER} does not exist yet. We'll create it.`);
            } else {
                console.log(`Error occurred while reading ${CUSTOM_CLASSPATH_REGISTER}`);
                // todo: exit?
            }
        }

        return mapFromFile;
    }

    async writeToJson(json: Object) {
        const writeFilePromise = util.promisify(fs.writeFile);

        try {
            await writeFilePromise(CUSTOM_CLASSPATH_REGISTER, JSON.stringify(json, null, 4));
            console.log(`Created language mapping: ${CUSTOM_CLASSPATH_REGISTER}`);
        } catch (err) {
            console.log(`Could not write to ${CUSTOM_CLASSPATH_REGISTER}: ${err.message}`);
        }
        
    }

    private mapToJson(inputMap: Map<string, string[]>) {
        let json = {};

        inputMap.forEach((value, key) => {
            json[key] = value;
        });

        return json;
    }

    private jsonToMap(json: Object) {
        return new Map(Object.entries(json));
    }
}
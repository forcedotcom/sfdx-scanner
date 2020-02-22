import fs = require('fs');
import path = require('path');
import util = require('util');

export const CUSTOM_CLASSPATH_REGISTER = path.join('catalogs', '.CustomPaths.json');

/**
 * Handles registering classpaths for custom rules into CustomPaths JSON
 * 
 * TODO: verify validity of path. Better to fail now than later while creating catalog.
 */


export enum Engine {
    PMD = "pmd"
}


export class CustomClasspathRegistrar {

    jsonHandler: RegistryJsonHandler;

    constructor() {
        this.jsonHandler = new RegistryJsonHandler();
    }

    public async createEntries(language: string, pathsInput: string[]): Promise<void> {

        const engine = this.identifyEngine();
        language = language.toLowerCase();

        // Fetch current entries in custom rule register as a Map
        const engineToLanguageMap = await this.jsonHandler.readCurrentEntries();
    
        this.createEngineToLanguageEntry(engineToLanguageMap, engine, language, pathsInput);
        
        // Write updated Map to file
        await this.jsonHandler.updateEntries(engineToLanguageMap);

    }

    public async getEntriesForEngine(engine: Engine): Promise<Map<string, string[]>> {
        const engineToLanguageMap = await this.jsonHandler.readCurrentEntries();
        if (engineToLanguageMap.has(engine)) {
            return engineToLanguageMap.get(engine);
        }

        // If engine is not found, return an empty Map rather than a null
        return new Map<string, string[]>();
    }

    public async getEntriesForLanguageByEngine(engine: Engine, language: string): Promise<string[]> {
        const engineToLanguageMap = await this.jsonHandler.readCurrentEntries();
        if (engineToLanguageMap.has(engine)) {
            const languageToPathMap = engineToLanguageMap.get(engine);
            if (languageToPathMap.has(language)) {
                return languageToPathMap.get(language);
            }
        }

        return [];
    }

    private createEngineToLanguageEntry(
        engineToLanguageMap: Map<string, Map<string, string[]>>, 
        engine: string, 
        language: string, 
        pathsInput: string[]) {

        let languageToPathMap: Map<string, string[]>;
        if (engineToLanguageMap.has(engine)) {
            // If given engine has entries, fetch existing language to path map
            languageToPathMap = engineToLanguageMap.get(engine)
        }
        else {
            // When current engine does not exist, create a new entry
            languageToPathMap = new Map<string, string[]>();
            engineToLanguageMap.set(engine, languageToPathMap);
        }

        // At nested level, add language to path entry
        this.createLanguageToPathEntry(languageToPathMap, language, pathsInput);
    }

    private createLanguageToPathEntry(
        languageToPathMap: Map<string, string[]>,
        language: string, 
        pathsInput: string[]) {

        if (languageToPathMap.has(language)) {

            // If given language has entries, append new paths to existing path array
            const paths = languageToPathMap.get(language);
            pathsInput.forEach((item) => {
                paths.push(item);
            });
        }
        else {
            // When current language does not exist, create a new entry
            languageToPathMap.set(language, pathsInput);
        }
    }

    private identifyEngine(): string {
        // For now, this logic is incomplete and always assumes PMD
        return Engine.PMD;
    }

 
}

class RegistryJsonHandler {

    jsonFileOperator: JsonFileOperator;

    constructor() {
        this.jsonFileOperator = new JsonFileOperator();
    }

    async readCurrentEntries(): Promise<Map<string, Map<string, string[]>>> {
        const jsonData = await this.jsonFileOperator.readJsonFile();
        const mapFromFile = this.jsonToMap(jsonData);

        return mapFromFile;
    }

    async updateEntries(engineToLanguageMap: Map<string, Map<string, string[]>>) {
        const jsonObj = this.mapToJson(engineToLanguageMap);
        await this.jsonFileOperator.writeToJsonFile(jsonObj);   
    }

    /**
     * Sample JSON for reference:
     * {
     *  "engine1": {
     *      "language1": ["/path/to/resource1", "path/to/resource2"],
     *      "language2": ["/path/to/another/resource"]
     *  },
     *  "engine2": {
     *      "language2": ["/yet/another/path"]
     *  }
     * }
     * 
     * This is converted into a Map such as:
     * engine ==> {language ==> paths[]}
     */

    /**
     * Converts Registry map into JSON object
     */
    private mapToJson(engineToLanguageMap: Map<string, Map<string, string[]>>) {
        let engineLevelJson = {};

        engineToLanguageMap.forEach((languageToPathMap, engine) => {
            let languageLevelJson = {}
            languageToPathMap.forEach((paths, language) => {
                languageLevelJson[language] = paths;
            });

            engineLevelJson[engine] = languageLevelJson;
        });

        return engineLevelJson;
    }

    /**
     * Converts JSON object into Registry map
     */
    private jsonToMap(jsonObj: Object) {
        // Fetch json entries at engine level
        const firstLevelMap = new Map(Object.entries(jsonObj));

        // Keep a return value ready
        let engineToLanguageMap = new Map<string, Map<string, string[]>>();

        firstLevelMap.forEach((secondLevelMapAsValue, engine) => {
            // Fetch json entries at language level
            const languageToPathMap = new Map(Object.entries(secondLevelMapAsValue));

            // Add engine to language mapping to return value
            engineToLanguageMap.set(engine, languageToPathMap);
        });

        return engineToLanguageMap;
    }
}

class JsonFileOperator {
    async readJsonFile() {
        const readFilePromise = util.promisify(fs.readFile);
        try {
            const data = await readFilePromise(CUSTOM_CLASSPATH_REGISTER, "utf-8");
            const jsonData = JSON.parse(data);
            return jsonData;
        }
        catch (err) {
            if (err.code = 'ENOENT') {
                console.log(`${CUSTOM_CLASSPATH_REGISTER} does not exist yet. Creating it.`);
            }
            else {
                console.log(`Error occurred while reading ${CUSTOM_CLASSPATH_REGISTER}`);
                // todo: throw error to display to user
            }
        }
    }

    async writeToJsonFile(jsonObj: {}) {
        const writeFilePromise = util.promisify(fs.writeFile);
        try {
            await writeFilePromise(CUSTOM_CLASSPATH_REGISTER, JSON.stringify(jsonObj, null, 4));
            console.log(`Created language mapping: ${CUSTOM_CLASSPATH_REGISTER}`);
        }
        catch (err) {
            console.log(`Could not write to ${CUSTOM_CLASSPATH_REGISTER}: ${err.message}`);
        }
    }
}
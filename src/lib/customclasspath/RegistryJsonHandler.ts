import fs = require('fs');
import path = require('path');
import util = require('util');


export const CUSTOM_CLASSPATH_REGISTER = path.join('pmd-cataloger', 'catalogs', '.CustomPaths.json');
export class RegistryJsonHandler {

    private jsonFileOperator: JsonFileOperator;

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
            engineLevelJson[engine] = this.getJsonOfLanguageMap(languageToPathMap);
        });

        return engineLevelJson;
    }


    getJsonOfLanguageMap(languageToPathMap: Map<string, string[]>) {
        let languageLevelJson = {}
        languageToPathMap.forEach((paths, language) => {
            languageLevelJson[language] = paths;
        });

        return languageLevelJson;
    }

    /**
     * Converts JSON object into Registry map
     */
    private jsonToMap(jsonObj: Object) {
        let engineToLanguageMap = new Map<string, Map<string, string[]>>();

        if (jsonObj != null && jsonObj != undefined) {
            // Fetch json entries at engine level
            const firstLevelMap = new Map(Object.entries(jsonObj));


            firstLevelMap.forEach((secondLevelMapAsValue, engine) => {
                // Fetch json entries at language level
                const languageToPathMap = new Map(Object.entries(secondLevelMapAsValue));

                // Add engine to language mapping to return value
                engineToLanguageMap.set(engine, languageToPathMap);
            });
        }
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
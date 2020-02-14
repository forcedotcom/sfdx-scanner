// import { AnyJson } from '@salesforce/ts-types';
import { PMD_LIB, PMD_VERSION } from './PmdSupport';
import fs = require('fs');
import path = require('path');
import util = require('util');

const LANGUAGE_MAPPING_JSON = './catalogs/.language-mapping.json';



export class LanguageMappingCreator {


    createPmdMapping(language: string) {
        const pmdJarPath = path.join(PMD_LIB, this.deriveJarNameForLanguage(language));
        this.createMapping(language, [pmdJarPath]);
    }

    public async createMapping(language: string, location: string[]) {
        //TODO: handle non-jar locations

        var currentMappings = await this.readCurrentMappings();
        if (currentMappings.has(language)) {
            var currentValue = currentMappings.get(language);
            location.forEach((item) => {
                currentValue.push(item);
            });
        } else {
            currentMappings.set(language, location);
        }
        
        await this.writeToJson(this.mapToJson(currentMappings));

    }

    async readCurrentMappings(): Promise<Map<string, string[]>> {
        var mapFromFile = new Map<string, string[]>();

        const readFilePromise = util.promisify(fs.readFile);
        
        try {
            const data = await readFilePromise(LANGUAGE_MAPPING_JSON, "utf-8");
            const json = JSON.parse(data);
            mapFromFile = this.jsonToMap(json);
        } catch (err) {
            if (err.code = 'ENOENT') {
                console.log(`${LANGUAGE_MAPPING_JSON} does not exist yet. We'll create it.`);
            } else {
                console.log(`Error occurred while reading ${LANGUAGE_MAPPING_JSON}`);
                // todo: exit?
            }
        }

        return mapFromFile;
    }

    async writeToJson(json: Object) {
        const writeFilePromise = util.promisify(fs.writeFile);

        try {
            await writeFilePromise(LANGUAGE_MAPPING_JSON, JSON.stringify(json, null, 4));
            console.log(`Created language mapping: ${LANGUAGE_MAPPING_JSON}`);
        } catch (err) {
            console.log(`Could not write to ${LANGUAGE_MAPPING_JSON}: ${err.message}`);
        }
        
    }

    private mapToJson(inputMap: Map<string, string[]>) {
        var json = {};

        inputMap.forEach((value, key) => {
            json[key] = value;
        });

        return json;
    }

    private jsonToMap(json: Object) {
        return new Map(Object.entries(json));
    }

    private deriveJarNameForLanguage(language: string) {
        return "pmd-" + language + "-" + PMD_VERSION + ".jar";
    }
}
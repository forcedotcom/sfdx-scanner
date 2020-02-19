// import { AnyJson } from '@salesforce/ts-types';
import { PMD_LIB, PMD_VERSION } from './PmdSupport';
import fs = require('fs');
import path = require('path');
import util = require('util');

export const CUSTOM_RULE_REGISTER = './catalogs/.custom_rules.json';



export class CustomRuleRegistrar {


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
            const data = await readFilePromise(CUSTOM_RULE_REGISTER, "utf-8");
            const json = JSON.parse(data);
            mapFromFile = this.jsonToMap(json);
        } catch (err) {
            if (err.code = 'ENOENT') {
                console.log(`${CUSTOM_RULE_REGISTER} does not exist yet. We'll create it.`);
            } else {
                console.log(`Error occurred while reading ${CUSTOM_RULE_REGISTER}`);
                // todo: exit?
            }
        }

        return mapFromFile;
    }

    async writeToJson(json: Object) {
        const writeFilePromise = util.promisify(fs.writeFile);

        try {
            await writeFilePromise(CUSTOM_RULE_REGISTER, JSON.stringify(json, null, 4));
            console.log(`Created language mapping: ${CUSTOM_RULE_REGISTER}`);
        } catch (err) {
            console.log(`Could not write to ${CUSTOM_RULE_REGISTER}: ${err.message}`);
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
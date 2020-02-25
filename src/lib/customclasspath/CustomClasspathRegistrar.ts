import { RegistryJsonHandler } from './RegistryJsonHandler';

/**
 * Handles registering classpaths for custom rules and provides visibility into the registry
 *
 * TODO: How can I read the JSON file just once and reuse it everywhere?
 *
 * TODO: verify validity of path. Better to fail now than later while creating catalog.
 */


export enum Engine {
    PMD = 'pmd'
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

    public async getJsonBasedEntriesForEngine(engine: Engine): Promise<string> {
        const languageToPathMap = await this.getEntriesForEngine(engine);
        const jsonObject = this.jsonHandler.getJsonOfLanguageMap(languageToPathMap);
        return JSON.stringify(jsonObject);
    }

    private createEngineToLanguageEntry(
        engineToLanguageMap: Map<string, Map<string, string[]>>,
        engine: string,
        language: string,
        pathsInput: string[]) {

        let languageToPathMap: Map<string, string[]>;
        if (engineToLanguageMap.has(engine)) {
            // If given engine has entries, fetch existing language to path map
            languageToPathMap = engineToLanguageMap.get(engine);
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
            pathsInput.forEach((item) => paths.push(item));
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

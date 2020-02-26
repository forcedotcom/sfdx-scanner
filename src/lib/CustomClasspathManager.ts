import fs = require('fs');
import path = require('path');
import util = require('util');
import { SfdxError } from '@salesforce/core';

export const CUSTOM_CLASSPATH_REGISTER = path.join('catalogs', '.CustomPaths.json');

/**
 * Handles registering classpaths for custom rules and provides visibility into the registry
 *
 * TODO: How can I read the JSON file just once and reuse it everywhere?
 *
 * TODO: verify validity of path. Better to fail now than later while creating catalog.
 */


export enum ENGINE {
  PMD = 'pmd'
}


export class CustomClasspathManager {

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

  public async getEntriesForEngine(engine: ENGINE): Promise<Map<string, string[]>> {
    const engineToLanguageMap = await this.jsonHandler.readCurrentEntries();
    if (engineToLanguageMap.has(engine)) {
      return engineToLanguageMap.get(engine);
    }

    // If engine is not found, return an empty Map rather than a null
    return new Map<string, string[]>();
  }

  public async getJsonBasedEntriesForEngine(engine: ENGINE): Promise<string> {
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

  private identifyEngine(): ENGINE {
    // For now, this logic is incomplete and always assumes PMD
    // TODO: extend this method when we handle other Rule engines
    return ENGINE.PMD;
  }
}

class RegistryJsonHandler {
  private jsonFileOperator: JsonFileOperator;

  constructor() {
    this.jsonFileOperator = new JsonFileOperator();
  }

  async readCurrentEntries(): Promise<Map<string, Map<string, string[]>>> {
    const jsonData = await this.jsonFileOperator.readFromJsonFile();
    const mapFromFile = this.convertJsonToMap(jsonData);

    return mapFromFile;
  }

  async updateEntries(engineToLanguageMap: Map<string, Map<string, string[]>>) {
    const jsonObj = this.convertMapToJson(engineToLanguageMap);
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
  private convertMapToJson(engineToLanguageMap: Map<string, Map<string, string[]>>) {
    const engineLevelJson = {};

    engineToLanguageMap.forEach((languageToPathMap, engine) => {
      engineLevelJson[engine] = this.getJsonOfLanguageMap(languageToPathMap);
    });

    return engineLevelJson;
  }


  /**
   * This method is externally used to build JSON of language map.
   * TODO: Find a nicer home for this method so this can be used
   * internally by convertMapToJson as well as called from outside.
   */
  getJsonOfLanguageMap(languageToPathMap: Map<string, string[]>) {
    const languageLevelJson = {};
    languageToPathMap.forEach((paths, language) => {
      languageLevelJson[language] = paths;
    });

    return languageLevelJson;
  }

  /**
   * Converts JSON object into Registry map
   */
  private convertJsonToMap(jsonObj: object) {
    const engineToLanguageMap = new Map<string, Map<string, string[]>>();

    if (jsonObj !== null && jsonObj !== undefined) {
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
  async readFromJsonFile() {
    const readFilePromise = util.promisify(fs.readFile);
    try {
      const data = await readFilePromise(CUSTOM_CLASSPATH_REGISTER, 'utf-8');
      const jsonData = JSON.parse(data);
      return jsonData;
    }
    catch (err) {
      if (err.code === 'ENOENT') {
        console.log(`${CUSTOM_CLASSPATH_REGISTER} does not exist yet. Creating it.`);
      }
      else {
        console.log(`Error occurred while reading ${CUSTOM_CLASSPATH_REGISTER}`);// TODO: move to logger.trace
        throw SfdxError.create('scanner', 'add', 'errors.errorReadingCustomClasspath', [`${CUSTOM_CLASSPATH_REGISTER}`, `${err.message}`]);
      }
    }
  }

  async writeToJsonFile(jsonObj: {}) {
    const writeFilePromise = util.promisify(fs.writeFile);
    try {
      await writeFilePromise(CUSTOM_CLASSPATH_REGISTER, JSON.stringify(jsonObj, null, 4));
      console.log(`Created language mapping: ${CUSTOM_CLASSPATH_REGISTER}`);// TODO: move to logger.trace
    }
    catch (err) {
      console.log(`Could not write to ${CUSTOM_CLASSPATH_REGISTER}: ${err.message}`);// TODO: move to logger.trace
      throw SfdxError.create('scanner', 'add', 'errors.errorWritingCustomClasspath', [`${CUSTOM_CLASSPATH_REGISTER}`, `${err.message}`]);
    }
  }
}

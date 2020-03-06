import path = require('path');
import os = require('os');
import { FileHandler } from './FileHandler';
import { SfdxError } from '@salesforce/core';

export enum ENGINE {
  PMD = 'pmd'
}

type RulePathEntry = Map<string, Set<string>>;
type RulePathMap = Map<ENGINE, RulePathEntry>;

const CATALOG_PATH = path.join(os.homedir(), '.sfdx-scanner');
export const CUSTOM_CLASSPATH_REGISTER = path.join(CATALOG_PATH, 'CustomPaths.json');
export const CUSTOM_CLASSPATH_REGISTER_TMP = path.join(CATALOG_PATH, 'TmpCustomPaths.json');

const EMPTY_JSON_FILE = '{}';

export class CustomRulePathManager {
  private pathsByLanguageByEngine: RulePathMap;
  private initialized: boolean;
  private fileHandler: FileHandler;

  constructor() {
    this.pathsByLanguageByEngine = new Map();
    this.fileHandler = new FileHandler();
    this.initialized = false;
  }

  private async initialize(): Promise<void> {
    if (this.initialized) {
      return;
    }

    // Read from the JSON and use it to populate the map.
    let data = null;
    try {
      data = await this.fileHandler.readFile(CUSTOM_CLASSPATH_REGISTER);
    } catch (e) {
      // An ENOENT error is fine, because it just means the file doesn't exist yet. We'll respond by spoofing a JSON with
      // no information in it.
      if (e.code === 'ENOENT') {
        data = EMPTY_JSON_FILE;
      } else {
        //  Any other error needs to be rethrown, and since it could be arcane or weird, we'll also prepend it with a
        //  header so it's clear where it came from.
        throw SfdxError.create('scanner', 'add', 'errors.readCustomRulePathFileFailed', [e.message]);
      }
    }
    // If file existed but was empty, replace the whitespace/blank with empty JSON
    if ('' === data.trim()) {
      data = EMPTY_JSON_FILE;
    }
    // Now that we've got the file contents, let's turn it into a JSON.
    const json = JSON.parse(data);
    this.pathsByLanguageByEngine = this.convertJsonDataToMap(json);
    this.initialized = true;
  }

  public async addPathsForLanguage(language: string, paths: string[]): Promise<void> {
    await this.initialize();
    // Identify the engine for each path and put them in the appropriate map and inner map.
    paths.forEach((path) => {
      const e = this.determineEngineForPath(path);
      if (!this.pathsByLanguageByEngine.has(e)) {
        this.pathsByLanguageByEngine.set(e, new Map());
      }
      if (!this.pathsByLanguageByEngine.get(e).has(language)) {
        this.pathsByLanguageByEngine.get(e).set(language, new Set([path]));
      } else {
        this.pathsByLanguageByEngine.get(e).get(language).add(path);
      }
    });
    // Now, write the changes to the file.
    return await this.saveCustomClasspaths();
  }

  public async getRulePathEntries(engine: ENGINE): Promise<Map<string, Set<string>>> {
    await this.initialize();

    if (!this.pathsByLanguageByEngine.has(engine)) {
      return new Map();
    }
    
    return this.pathsByLanguageByEngine.get(engine);
  }

  private async saveCustomClasspaths(): Promise<void> {
    await this.initialize();
    try {
      const fileContent = JSON.stringify(this.convertMapToJson(), null, 4)
      await this.fileHandler.mkdirIfNotExists(CATALOG_PATH);
      await this.fileHandler.writeFile(CUSTOM_CLASSPATH_REGISTER, fileContent);
      
    } catch (e) {
      // If the write failed, the error might be arcane or confusing, so we'll want to prepend the error with a header
      // so it's at least obvious what failed, if not how or why.
      throw SfdxError.create('scanner', 'add', 'errors.writeCustomRulePathFileFailed', [e.message]);
    }
  }

  private convertJsonDataToMap(json): RulePathMap {
    const map = new Map();
    for (const key of Object.keys(json)) {
      const engine = key as ENGINE;
      const val = json[key];
      const innerMap = new Map();
      for (const lang of Object.keys(val)) {
        innerMap.set(lang, new Set(val[lang]));
      }
      map.set(engine, innerMap);
    }
    return map;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private determineEngineForPath(path: string): ENGINE {
    // TODO: Once we support other engines, we'll need more logic here.
    return ENGINE.PMD;
  }

  private convertMapToJson(): object {
    const json = {};
    this.pathsByLanguageByEngine.forEach((pathsByLang, engine) => {
      const innerObj = {};
      pathsByLang.forEach((paths, lang) => {
        innerObj[lang] = Array.from(paths);
      });
      json[engine.toString()] = innerObj;
    });
    return json;
  }
}


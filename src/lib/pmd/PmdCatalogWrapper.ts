import { AnyJson } from '@salesforce/ts-types';
import fs = require('fs');
import { RegistryJsonHandler } from '../customclasspath/RegistryJsonHandler';
import { Rule } from '../../types';
import { RuleFilter, RULE_FILTER_TYPE } from '../RuleManager';
import { PmdSupport, PMD_LIB, PMD_VERSION } from './PmdSupport';

const PMD_CATALOGER_LIB = './dist/pmd-cataloger/lib';

const SUPPORTED_LANGUAGES = ['apex', 'javascript'];
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export type PmdCatalog = {
  rules: Rule[];
  categories: AnyJson[];
  rulesets: AnyJson[];
};

export class PmdCatalogWrapper extends PmdSupport {
  private catalogJson : PmdCatalog;
  constructor() {
    super();
  }

  public async getCatalog() : Promise<PmdCatalog> {
    // If we haven't read in a catalog yet, do so now.
    if (!this.catalogJson) {
      await this.rebuildCatalogIfNecessary();
      this.catalogJson = PmdCatalogWrapper.readCatalogFromFile();
    }
    return Promise.resolve(this.catalogJson);
  }

  /**
   * Accepts a set of filter criteria, and returns the paths of all categories and rulesets matching those criteria.
   * @param {RuleFilter[]} filters
   */
  public async getPathsMatchingFilters(filters: RuleFilter[]) : Promise<string[]> {
    // If we haven't read in a catalog yet, do so now.
    if (!this.catalogJson) {
      await this.rebuildCatalogIfNecessary();
      this.catalogJson = PmdCatalogWrapper.readCatalogFromFile();
    }
    // Now that we've got a catalog, we'll want to iterate over all the filters we were given, and see which ones
    // correspond to a path in the catalog.
    // Since PMD treats categories and rulesets as interchangeable inputs, we can put both types of path into a single
    // array and return that.
    let paths = [];
    filters.forEach(filter => {
      // Since PMD accepts rulesets and categories instead of individual rules, we only care about filters that act on
      // rulesets and categories.
      const type = filter.filterType;
      if (type === RULE_FILTER_TYPE.CATEGORY || type === RULE_FILTER_TYPE.RULESET) {
        // We only want to evaluate category filters against category names, and ruleset filters against ruleset names.
        const subcatalog = type === RULE_FILTER_TYPE.CATEGORY ? this.catalogJson.categories : this.catalogJson.rulesets;
        filter.filterValues.forEach(value => {
          // If there's a matching category/ruleset for the specified filter, we'll need to add all the corresponding paths
          // to our list.
          if (subcatalog[value]) {
            paths = [...paths, ...subcatalog[value]];
          }
        });
      }
    });
    return paths;
  }

  private async rebuildCatalogIfNecessary(): Promise<[boolean,string]> {
    // First, check whether the catalog is stale. If it's not, we don't even need to do anything.
    if (!PmdCatalogWrapper.catalogIsStale()) {
      return new Promise<[boolean,string]>(() => [false, 'no action taken']);
    }

    return this.runCommand();
  }

  private static catalogIsStale(): boolean {
    // TODO: Pretty soon, we'll want to add sophisticated logic to determine whether the catalog is stale. But for now,
    //  we'll just return true so we always rebuild the catalog.
    return true;
  }

  private static readCatalogFromFile(): PmdCatalog {
    const rawCatalog = fs.readFileSync('./catalogs/PmdCatalog.json');
    return JSON.parse(rawCatalog.toString());
  }

  protected async buildCommandArray(): Promise<[string, string[]]> {
    const command = 'java';

    let customClasspathJsonString = await this.getCustomClasspathMapping();
    customClasspathJsonString = encodeURI(customClasspathJsonString);
    const classpath = await this.buildClasspath();

    // NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
    // is intended for child_process.spawn(), which freaks out if you do that.
    const args = ['-cp', classpath.join(':'), MAIN_CLASS, PMD_LIB, PMD_VERSION, SUPPORTED_LANGUAGES.join(','), customClasspathJsonString];

    console.log(`Arguments to be passed with command: ${args}`);
    return [command, args];
  }

  protected async buildClasspath(): Promise<string[]> {
    // TODO: This probably isn't where the JAR ought to live. Once the JAR's home is finalized, come back to this.
    const catalogerLibs = `${PMD_CATALOGER_LIB}/*`;
    const classpath = await super.buildClasspath();
    return classpath.concat([catalogerLibs]);
  }

  protected async getCustomClasspathMapping(): Promise<string> {
    const customPathEntries = await super.getCustomPathEntriesForPmd();

    const registryJsonHandler = new RegistryJsonHandler();
    const jsonObject = registryJsonHandler.getJsonOfLanguageMap(customPathEntries);

    if (jsonObject !== null || jsonObject !== undefined) {
      return JSON.stringify(jsonObject);
    }

    return '{}';
  }
}

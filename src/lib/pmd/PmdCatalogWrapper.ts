import {AnyJson} from '@salesforce/ts-types';
import path = require('path');
import * as JreSetupManager from './../JreSetupManager';
import {Rule} from '../../types';
import {RuleFilter, RULE_FILTER_TYPE} from '../RuleManager';
import {PmdSupport, PMD_LIB, PMD_VERSION} from './PmdSupport';
import {FileHandler} from '../util/FileHandler';
import {PMD_CATALOG, SFDX_SCANNER_PATH} from '../../Constants';
import {ChildProcessWithoutNullStreams} from "child_process";
import { Logger } from '@salesforce/core';
import {OutputProcessor} from './OutputProcessor';

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/pmd
const PMD_CATALOGER_LIB = path.join(__dirname, '..', '..', '..', 'dist', 'pmd-cataloger', 'lib');
const SUPPORTED_LANGUAGES = ['apex', 'javascript'];
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export type PmdCatalog = {
  rules: Rule[];
  categories: AnyJson[];
  rulesets: AnyJson[];
};

export class PmdCatalogWrapper extends PmdSupport {
  private catalogJson: PmdCatalog;
  private outputProcessor: OutputProcessor;
  private logger: Logger; // TODO: add relevant trace logs

  protected async init(): Promise<void> {
    this.outputProcessor = await OutputProcessor.create({});
    this.logger = await Logger.child('PmdCatalogWrapper');
  }

  public async getCatalog(): Promise<PmdCatalog> {
    // If we haven't read in a catalog yet, do so now.
    if (!this.catalogJson) {
      await this.rebuildCatalogIfNecessary();
      this.catalogJson = await PmdCatalogWrapper.readCatalogFromFile();
    }
    return Promise.resolve(this.catalogJson);
  }

  /**
   * Accepts a set of filter criteria, and returns the paths of all categories and rulesets matching those criteria.
   * @param {RuleFilter[]} filters
   */
  public async getPathsMatchingFilters(filters: RuleFilter[]): Promise<string[]> {
    // If we haven't read in a catalog yet, do so now.
    if (!this.catalogJson) {
      await this.rebuildCatalogIfNecessary();
      this.catalogJson = await PmdCatalogWrapper.readCatalogFromFile();
    }
    // If we weren't given any filters, that should be treated as implicitly including all rules. Since PMD defines its
    // rules in category files, we should just return all paths corresponding to a category.
    if (!filters || filters.length === 0) {
      return this.getAllCategoryPaths();
    }
    // If we actually do have filters, we'll want to iterate over all of them and see which ones correspond to a path in
    // the catalog.
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

  private getAllCategoryPaths(): string[] {
    // We'll want to be building a list of all of the paths we found.
    let paths = [];
    // Since this method is run when no filter criteria are provided, it might be nice to provide a level of visibility
    // into all of the categories that were run. To do that, we'll be creating event descriptors as we go.
    const events = [];

    // Iterate over all of the categories.
    const categories = this.catalogJson.categories;
    Object.getOwnPropertyNames(categories).forEach((catName) => {
      const catPaths = categories[catName];
      // Add all of the paths associated with this category name into our list.
      paths = [...paths, ...catPaths];
      // For each path, build an event indicating that the path was implicitly included.
      catPaths.forEach((path) => {
        events.push({
          messageKey: 'info.pmdJarImplicitlyRun',
          args: [catName, path],
          type: 'INFO',
          handler: 'UX',
          verbose: true,
          time: Date.now()
        });
      });
    });

    // Throw all of our events, and then return the paths.
    this.outputProcessor.emitEvents(events);
    return paths;
  }

  private static getCatalogName(): string {
    // We must allow for env variables to override the default catalog name. This must be recomputed in case those variables
    // have different values in different test runs.
    return process.env.PMD_CATALOG_NAME || PMD_CATALOG;
  }

  private static getCatalogPath(): string {
    return path.join(SFDX_SCANNER_PATH, this.getCatalogName());
  }

  private static async readCatalogFromFile(): Promise<PmdCatalog> {
    const rawCatalog = await new FileHandler().readFile(this.getCatalogPath());
    return JSON.parse(rawCatalog);
  }

  protected async buildCommandArray(): Promise<[string, string[]]> {
    const javaHome = await JreSetupManager.verifyJreSetup();
    const command = path.join(javaHome, 'bin', 'java');
    
    // NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
    // is intended for child_process.spawn(), which freaks out if you do that.
    const [classpathEntries, parameters] = await Promise.all([this.buildClasspath(), this.buildCatalogerParameters()]);
    const args = [`-DcatalogName=${PmdCatalogWrapper.getCatalogName()}`, '-cp', classpathEntries.join(':'), MAIN_CLASS, ...parameters];

    // TODO: move as log line to Trace
    // console.log(`About to invoke Cataloger with args: ${args}`);
    return [command, args];
  }

  protected async buildClasspath(): Promise<string[]> {
    const catalogerLibs = `${PMD_CATALOGER_LIB}/*`;
    const classpathEntries = await super.buildClasspath();
    classpathEntries.push(catalogerLibs);
    return classpathEntries;
  }

  private async buildCatalogerParameters(): Promise<string[]> {
    // Get custom rule path entries
    const rulePathEntries = await this.getRulePathEntries();

    // Add inbuilt PMD rule path entries
    this.addPmdJarPaths(rulePathEntries);

    const parameters = [];
    const divider = '=';
    const joiner = ',';

    // For each language, build an argument that looks like:
    // "language=path1,path2,path3"
    rulePathEntries.forEach((entries, language) => {
      const paths = Array.from(entries.values());
      parameters.push(language + divider + paths.join(joiner));
    });

    return parameters;
  }

  private addPmdJarPaths(rulePathEntries: Map<string, Set<string>>): void {
    // For each supported language, add path to PMD's inbuilt rules
    SUPPORTED_LANGUAGES.forEach((language) => {
      const pmdJarName = this.derivePmdJarName(language);

      // TODO: logic to add entries should be encapsulated away from here. 
      // Duplicates some logic in CustomRulePathManager. Consider refactoring
      if (!rulePathEntries.has(language)) {
        rulePathEntries.set(language, new Set<string>());
      }
      rulePathEntries.get(language).add(pmdJarName);
    });
    this.logger.trace(`Added PMD Jar paths: ${rulePathEntries}`);
  }


  /**
   * PMD library holds the same naming structure for each language
   */
  private derivePmdJarName(language: string): string {
    return path.join(PMD_LIB, "pmd-" + language + "-" + PMD_VERSION + ".jar");
  }

  /**
   * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject function.
   * Resolves/rejects the Promise once the child process finishes.
   * @param cp
   * @param res
   * @param rej
   */
  protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: ([boolean, string]) => void, rej: (string) => void): void {
    let stdout = '';
    let stderr = '';

    // When data is passed back up to us, pop it onto the appropriate string.
    cp.stdout.on('data', data => {
      stdout += data;
    });
    cp.stderr.on('data', data => {
      stderr += data;
    });

    // When the child process exits, if it exited with a zero code we can resolve, otherwise we'll reject.
    cp.on('exit', code => {
      this.outputProcessor.processOutput(stdout);

      if (code === 0) {
        res([!!code, stdout]);
      }
      else {
        rej(stderr);
      }
    });
  }


}

import {Rule} from '../../types';
import {AnyJson} from '@salesforce/ts-types';
import {PmdSupport, PMD_LIB, PMD_VERSION} from './PmdSupport';
import fs = require('fs');

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

  private async rebuildCatalogIfNecessary(): Promise<string> {
    // First, check whether the catalog is stale. If it's not, we don't even need to do anything.
    if (!PmdCatalogWrapper.catalogIsStale()) {
      return Promise.resolve('no action taken');
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

  protected buildCommand(): string {
    // TODO: We'll need to make sure this works on Windows.
    return `java -cp "${this.buildClasspath().join(':')}" ${MAIN_CLASS} ${PMD_LIB} ${PMD_VERSION} ${SUPPORTED_LANGUAGES.join(',')}`;
  }

  protected buildClasspath(): string[] {
    // TODO: This probably isn't where the JAR ought to live. Once the JAR's home is finalized, come back to this.
    const catalogerLibs = `${PMD_CATALOGER_LIB}/*`;
    return super.buildClasspath().concat([catalogerLibs]);
  }
}

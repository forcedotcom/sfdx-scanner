import { AnyJson } from '@salesforce/ts-types';
import child_process = require('child_process');
import fs = require('fs');

// TODO: Make these OS-agnostic and dynamic.
const PMD_LIB = './dist/pmd/lib';
const PMD_VERSION = '6.20.0';
const SUPPORTED_LANGUAGES = ['apex', 'javascript'];
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export type PmdCatalog = {
  rules: AnyJson[];
  categories: AnyJson[];
  rulesets: AnyJson[];
};

export class PmdCatalogWrapper {
  constructor() {

  }

  public async getCatalog() : Promise<PmdCatalog> {
    return this.rebuildCatalogIfNecessary()
      .then(() => {
        return PmdCatalogWrapper.readCatalogFromFile();
      },stderr => {
        return Promise.reject(stderr);
      });
  }

  private async rebuildCatalogIfNecessary() : Promise<string> {
    // First, check whether the catalog is stale. If it's not, we don't even need to do anything.
    if (!PmdCatalogWrapper.catalogIsStale()) {
      return Promise.resolve('no action taken');
    }

    const command = PmdCatalogWrapper.buildCommand();
    return new Promise((res, rej) => {
      child_process.exec(command, (err, stdout, stderr) => {
        if (err) {
          rej(stderr);
        } else {
          res('successfully rebuilt catalog');
        }
      });
    });
  }

  private static catalogIsStale() : boolean {
    // TODO: Pretty soon, we'll want to add sophisticated logic to determine whether the catalog is stale. But for now,
    //  we'll just return true so we always rebuild the catalog.
    return true;
  }

  private static buildCommand() : string {
    // TODO: We'll need to make sure this works on Windows.
    return `java -cp "${PmdCatalogWrapper.buildClasspath()}" ${MAIN_CLASS} ${PMD_LIB} ${PMD_VERSION} ${SUPPORTED_LANGUAGES.join(',')}`;
  }

  private static buildClasspath() : string {
    // TODO: This probably isn't where the JAR ought to live. Once the JAR's home is finalized, come back to this.
    const catalogerLibs = './pmd-cataloger/build/install/pmd-cataloger/lib/*';
    const pmdPath = PMD_LIB + '/*';
    // TODO: We want to allow users to add their own PMD rules, so we'll need some way for them to submit them.

    // Because the classpath is a Java convention, we can use the same syntax across platforms, which is nice.
    return [catalogerLibs, pmdPath].join(':');
  }

  private static readCatalogFromFile() : PmdCatalog {
    const rawCatalog = fs.readFileSync('./catalogs/PmdCatalog.json');
    return JSON.parse(rawCatalog.toString());
  }
}

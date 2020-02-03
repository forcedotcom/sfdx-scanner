import { AnyJson } from '@salesforce/ts-types';
import child_process = require('child_process');

// TODO: Make these OS-agnostic and dynamic.
const PMD_LIB = "./dist/pmd/lib";
const PMD_VERSION = "6.20.0";
const SUPPORTED_LANGUAGES = ["apex", "javascript"];
const MAIN_CLASS = "sfdc.isv.swat.Main";

export default class PmdCatalogWrapper {
  constructor() {

  }

  public async getCatalog() : Promise<AnyJson> {
    return this.rebuildCatalogIfNecessary()
      .then(() => {
        return this.readCatalogFromFile();
      }, () => {
        return Promise.reject({});
      });
  }

  private async readCatalogFromFile() : Promise<AnyJson> {
    // TODO: Replace this hardcoded crap with real results.
    let rules = [
      {
        name: 'Rule 1',
        categories: ['Best Practice', 'Code Styling'],
        rulesets: ['core/best-practice'],
        languages: ['JS', 'Apex', 'Java'],
        author: 'Salesforce'
      },
      {
        name: 'Rule 2',
        categories: ['Security', 'XSS'],
        rulesets: ['lib/security'],
        languages: ['JS'],
        author: 'Doofenshmirtz Evil Inc'
      }
    ];
    return new Promise((res, rej) => {
      setTimeout(() => {
        res(rules);
      }, 2500);
    });
  }

  private async rebuildCatalogIfNecessary() : Promise<void> {
    // First, check whether the catalog is stale. If it's not, we don't even need to do anything.
    if (!this.catalogIsStale()) {
      return Promise.resolve();
    }

    const command = this.buildCommand();
    return new Promise((res, rej) => {
      child_process.exec(command, (err, stdout, stderr) => {
        if (err) {
          // TODO: Better error handling needed.
          console.log("Error caught in PmdCatalogWrapper.ts: " + (err.message || err) + " " + (err.code || "no code"));
          rej(err);
        } else {
          console.log("PmdCatalogWrapper.ts built catalog just fine.");
          console.log(stdout);
          res();
        }
      });
    });
  }

  private catalogIsStale() : boolean {
    // TODO: Pretty soon, we'll want to add sophisticated logic to determine whether the catalog is stale. But for now,
    //  we'll just return true so we always rebuild the catalog.
    return true;
  }

  private buildCommand() : string {
    return `java -cp "${this.buildClasspath()}" ${MAIN_CLASS} ${PMD_LIB} ${PMD_VERSION} ${SUPPORTED_LANGUAGES.join(',')}`;
  }

  private buildClasspath() : string {
    // TODO: Update this once we build the cataloger to a big-boy path.
    const catalogerPath = './out/production/main';
    const pmdPath = PMD_LIB + "/*";
    const jsonPath = './dist/json-simple/*';

    // TODO: Classpaths might be formatted differently in Windows. Change this to something that will work in both Windows
    // and Unix.
    return [catalogerPath, pmdPath, jsonPath].join(':');
  }
}

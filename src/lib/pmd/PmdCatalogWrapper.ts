import child_process = require('child_process');

// TODO: Make these OS-agnostic and dynamic.
const PMD_LIB = "./dist/pmd/lib";
const PMD_VERSION = "6.20.0";
const SUPPORTED_LANGUAGES = ["apex", "javascript"];

export default class PmdCatalogWrapper {
  constructor() {

  }

  public catalogIsStale() : boolean {
    // TODO: Pretty soon, we'll want to add sophisticated logic to determine whether the catalog is stale. But for now,
    //  we'll just return true so we always rebuild the catalog.
    return true;
  }

  public async rebuildCatalog() : Promise<string> {
    return new Promise((res, rej) => {
      const classPath = this.buildClasspath();
      const command = `java -cp "${classPath}" sfdc.isv.swat.Main ${PMD_LIB} ${PMD_VERSION} ${SUPPORTED_LANGUAGES.join(',')}`;
      child_process.exec(command, (err, stdout, stderr) => {
        if (err) {
          console.log('Error occurred during catalog build: ' + err);
          rej(err.message || err);
        } else {
          console.log('Everything was fine');
          res('success');
        }
      });
    });
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

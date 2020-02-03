import child_process = require('child_process');

export default class PmdCatalogWrapper {
  constructor() {

  }

  public async buildCatalog() : Promise<string> {
    return new Promise((res, rej) => {
      child_process.exec("java -cp \"./out/production/main:./dist/pmd/lib/*:./dist/json-simple/*\" sfdc.isv.swat.Main", (err, stdout, stderr) => {
        if (err) {
          console.log('Error occurred during catalog build: ' + err);
          rej(err);
        } else {
          console.log('everything was fine');
          res('success');
        }
      })
    });
  }
}

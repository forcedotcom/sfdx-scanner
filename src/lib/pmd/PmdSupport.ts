import child_process = require('child_process');

export const PMD_VERSION = '6.21.0';
export const PMD_LIB = './dist/pmd/lib';

/**
 * Output format supported by PMD
 */
export enum Format {
  XML = 'xml',
  CSV = 'csv',
  TEXT = 'txt'
}

export abstract class PmdSupport {

  protected buildClasspath(): string[] {
    const pmdLibs = `${PMD_LIB}/*`;
    // TODO: We want to allow users to add their own PMD rules, so we'll need some way for them to submit them.
    return [pmdLibs];
  }

  protected abstract buildCommand(): string;

  protected async runCommand(): Promise<string> {
    const command = this.buildCommand();
    return new Promise((res, rej) => {
      child_process.exec(command, (err, stdout, stderr) => {
        if (err) {
          rej(stderr);
        } else if (stdout) {
          res(stdout);
        } else {
          res('success');
        }
      });
    });
  }
}

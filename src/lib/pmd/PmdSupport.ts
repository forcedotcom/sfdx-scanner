import child_process = require('child_process');

export const PMD_VERSION = '6.21.0';
export const PMD_LIB = './dist/pmd/lib';

/**
 * Output format supported by PMD
 */
export enum Format {
  XML = 'xml',
  CSV = 'csv',
  TEXT = 'text'
}

export type PmdSupportCallback = (err: child_process.ExecException, stdout: string, stderr: string) => void;

export abstract class PmdSupport {

  protected buildClasspath(): string[] {
    const pmdLibs = `${PMD_LIB}/*`;
    // TODO: We want to allow users to add their own PMD rules, so we'll need some way for them to submit them.
    return [pmdLibs];
  }

  /**
   * Provides the default callback that should be provided for calls to child_process.exec. Extensions of this class can
   * override to add more nuanced behavior.
   * @param {Function} res - The 'resolve' method for a Promise.
   * @param {Function} rej - The 'reject' method for a Promise.
   */
  protected getCallback(res, rej) : PmdSupportCallback {
    return (err, stdout, stderr) => {
      if (err) {
        rej(stderr);
      } else if (stdout) {
        res(stdout);
      } else {
        res('success');
      }
    };
  }

  protected abstract buildCommand(): string;

  protected async runCommand(): Promise<string> {
    const command = this.buildCommand();
    return new Promise((res, rej) => {
      const callback = this.getCallback(res, rej);
      child_process.exec(command, callback);
    });
  }
}

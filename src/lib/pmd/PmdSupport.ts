import child_process = require('child_process');
import {ChildProcessWithoutNullStreams} from "child_process";

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

export abstract class PmdSupport {

  protected buildClasspath(): string[] {
    const pmdLibs = `${PMD_LIB}/*`;
    // TODO: We want to allow users to add their own PMD rules, so we'll need some way for them to submit them.
    return [pmdLibs];
  }

  /**
   * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
   * Resolves/rejects the Promise once the child process finishes.
   * @param cp
   * @param res
   * @param rej
   */
  protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: Function, rej: Function) : void {
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
      if (code === 0) {
        res(stdout);
      } else {
        rej(stderr);
      }
    });
  }

  protected abstract buildCommandArray(): [string, string[]];

  protected async runCommand(): Promise<any> {
    const [command, args] = this.buildCommandArray();

    return new Promise((res, rej) => {
      const cp = child_process.spawn(command, args);
      this.monitorChildProcess(cp, res, rej);
    });
  }
}

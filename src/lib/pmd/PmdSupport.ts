import childProcess = require('child_process');
import {ChildProcessWithoutNullStreams} from 'child_process';
import { CustomRulePathManager, ENGINE } from '../CustomRulePathManager'; 

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

  protected async buildClasspath(): Promise<string[]> {
    // Include PMD libs into classpath
    const pmdLibs = `${PMD_LIB}/*`;
    const classpathEntries = [pmdLibs];
    
    // Include custom rule paths into classpath
    const rulePathEntries = await this.getRulePathEntries();
    rulePathEntries.forEach((pathEntries, language) => {
      classpathEntries.push(...pathEntries);
    });

    return classpathEntries;
  }

  /**
   * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
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
      if (code === 0) {
        res([!!code, stdout]);
      } else {
        rej(stderr);
      }
    });
  }

  protected abstract buildCommandArray(): Promise<[string, string[]]>;

  protected async runCommand(): Promise<[boolean,string]> {
    const [command, args] = await this.buildCommandArray();

    return new Promise<[boolean,string]>((res, rej) => {
      const cp = childProcess.spawn(command, args);
      this.monitorChildProcess(cp, res, rej);
    });
  }

  protected async getRulePathEntries(): Promise<Map<string, Set<string>>> {
    const customRulePathManager = new CustomRulePathManager();
    return await customRulePathManager.getRulePathEntries(ENGINE.PMD);
  }
}

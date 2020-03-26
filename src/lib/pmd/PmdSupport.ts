import childProcess = require('child_process');
import {ChildProcessWithoutNullStreams} from 'child_process';
import { AsyncCreatable } from '@salesforce/kit';
import { CustomRulePathManager, ENGINE } from '../CustomRulePathManager'; 
import path = require('path');

export const PMD_VERSION = '6.22.0';
// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/pmd
export const PMD_LIB = path.join(__dirname,'..','..','..','dist','pmd','lib');

/**
 * Output format supported by PMD
 */
export enum Format {
  XML = 'xml',
  CSV = 'csv',
  TEXT = 'text'
}

export abstract class PmdSupport extends AsyncCreatable {

  protected async buildClasspath(): Promise<string[]> {
    // Include PMD libs into classpath
    const pmdLibs = `${PMD_LIB}/*`;
    const classpathEntries = [pmdLibs];
    
    // Include custom rule paths into classpath
    const rulePathEntries = await this.getRulePathEntries();
    rulePathEntries.forEach((pathEntries) => {
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
  protected abstract monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: ([boolean, string]) => void, rej: (string) => void): void;

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

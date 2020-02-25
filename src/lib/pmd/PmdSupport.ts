import child_process = require('child_process');
<<<<<<< HEAD
import {ChildProcessWithoutNullStreams} from "child_process";
import {CustomClasspathRegistrar, Engine} from "../customclasspath/CustomClasspathRegistrar";
=======
import {ChildProcessWithoutNullStreams} from 'child_process';
>>>>>>> 0abd387e0e7a1c36981ea37320663c21f56665aa

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
    let classpath = [];
    classpath.push(`${PMD_LIB}/*`);

    // Fetch classpaths derived from custom PMD rules
    const customClasspaths = await this.getCustomClasspath();
    classpath.push(...customClasspaths);
    
    return classpath;
  }

  /**
   * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
   * Resolves/rejects the Promise once the child process finishes.
   * @param cp
   * @param res
   * @param rej
   */
  protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: ([boolean, string]) => void, rej: (string) => void) : void {
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
      const cp = child_process.spawn(command, args);
      this.monitorChildProcess(cp, res, rej);
    });
  }

  protected async getCustomClasspath(): Promise<string[]> {
    let pmdClasspaths = [];

    const customPathEntries = await this.getCustomPathEntriesForPmd();
    customPathEntries.forEach((classpaths, language) => {
      pmdClasspaths.push(...classpaths);
    });

    return pmdClasspaths;
  }

  protected async getCustomPathEntriesForPmd(): Promise<Map<string, string[]>> {
    const classpathRegistrar = new CustomClasspathRegistrar();

    // TODO: find a way to keep this readily available so that we don't have
    // to read the entries every time.
    return await classpathRegistrar.getEntriesForEngine(Engine.PMD);
  }
}

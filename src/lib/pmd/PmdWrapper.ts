import {ChildProcessWithoutNullStreams} from 'child_process';
import {Logger} from '@salesforce/core';
import {Format, PmdSupport} from './PmdSupport';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

interface PmdWrapperOptions {
  path: string;
  rules: string;
  reportFormat?: Format;
  reportFile?: string;
}

export default class PmdWrapper extends PmdSupport {


  path: string;
  rules: string;
  reportFormat: Format;
  reportFile: string;
  logger: Logger; // TODO: Add relevant trace log lines

  protected async init(): Promise<void> {
    this.logger = await Logger.child('PmdWrapper');
  }

  public static async execute(path: string, rules: string, reportFormat?: Format, reportFile?: string): Promise<[boolean,string]> {
    const myPmd = await PmdWrapper.create({path: path, rules: rules, reportFormat: reportFormat, reportFile: reportFile});
    return myPmd.execute();
  }

  private async execute(): Promise<[boolean,string]> {
    return super.runCommand();
  }

  constructor(options: PmdWrapperOptions) {
    super(options);
    this.path = options.path;
    this.rules = options.rules;
    this.reportFormat = options.reportFormat || Format.XML;
    this.reportFile = options.reportFile || null;
  }

  protected async buildCommandArray(): Promise<[string, string[]]> {
    const javaHome = await JreSetupManager.verifyJreSetup();
    const command = path.join(javaHome, 'bin', 'java');

    // Start with the arguments we know we'll always need.
    // NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
    // is intended for child_process.spawn(), which freaks out if you do that.
    const classpath = await super.buildClasspath();
    let args = ['-cp', classpath.join(path.delimiter), HEAP_SIZE, MAIN_CLASS, '-rulesets', this.rules, '-dir', this.path,
      '-format', this.reportFormat];

    // Then add anything else that's dynamically included based on other input.
    if (this.reportFile) {
      args = [...args, '-reportfile', this.reportFile];
    }

    this.logger.trace(`Preparing to execute PMD with command: "${command}", args: "${args}"`);
    return [command, args];
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

    cp.on('exit', code => {
      this.logger.trace(`monitorChildProcess has received exit code ${code}`);
      if (code === 0 || code === 4) {
        // If the exit code is 0, then no rule violations were found. If the exit code is 4, then it means that at least
        // one violation was found. In either case, PMD ran successfully, so we'll resolve the Promise. We use a tuple
        // containing a boolean that will be true if there were any violations, and stdut.
        // That way, we have a simple indicator of whether there were violations, and a log that we can sweep to know
        // what those violations were.
        res([!!code, stdout]);
      } else {
        // If we got any other error, it means something actually went wrong. We'll just reject with stderr for the ease
        // of upstream error handling.
        rej(stderr);
      }
    });
  }
}

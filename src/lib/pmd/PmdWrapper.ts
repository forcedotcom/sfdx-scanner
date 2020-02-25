import {Format, PmdSupport} from './PmdSupport';
import {ChildProcessWithoutNullStreams} from "child_process";

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

export default class PmdWrapper extends PmdSupport {

  path: string;
  rules: string;
  reportFormat: Format;
  reportFile: string;
  customRuleJar: string;

  public static async execute(path: string, rules: string, reportFormat?: Format, reportFile?: string, customRuleJar?: string) {
    const myPmd = new PmdWrapper(path, rules, reportFormat, reportFile, customRuleJar);
    return myPmd.execute();
  }

  private async execute() {
    return super.runCommand();
  }

  constructor(path: string, rules: string, reportFormat?: Format, reportFile?: string, customRuleJar?: string) {
    super();
    this.path = path;
    this.rules = rules;
    this.reportFormat = reportFormat || Format.XML;
    this.reportFile = reportFile || null;
    this.customRuleJar = customRuleJar || null;
  }


  async getClassPath(): Promise<string[]> {
    var classPath = await super.buildClasspath();
    if (this.customRuleJar.length > 0) {
        // TODO: verify that jar file exists
      classPath.push(this.customRuleJar);
    }
    return classPath;
}

  protected async buildCommandArray(): Promise<[string, string[]]> {
    const command = 'java';
    const classpath = await this.getClassPath();
    // Start with the arguments we know we'll always need.
    // NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
    // is intended for child_process.spawn(), which freaks out if you do that.
    let args = ['-cp', classpath.join(':'), HEAP_SIZE, MAIN_CLASS, '-rulesets', this.rules, '-dir', this.path,
      '-format', this.reportFormat];


    // Then add anything else that's dynamically included based on other input.
    if (this.reportFile) {
      args = [...args, '-reportfile', this.reportFile];
    }

    return [command, args];
  }

  /**
   * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
   * Resolves/rejects the Promise once the child process finishes.
   * @param cp
   * @param res
   * @param rej
   */
  protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: Function, rej: Function): void {
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

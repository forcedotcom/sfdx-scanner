import {Format, PmdSupport, PmdSupportCallback} from './PmdSupport';

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

export default class PmdWrapper extends PmdSupport {

  path: string;
  rules: string;
  reportFormat: Format;
  reportFile: string;

  public static async execute(path: string, rules: string, reportFormat?: Format, reportFile?: string) {
    const myPmd = new PmdWrapper(path, rules, reportFormat, reportFile);
    return myPmd.execute();
  }

  private async execute() {
    return super.runCommand();
  }

  constructor(path: string, rules: string, reportFormat?: Format, reportFile?: string) {
    super();
    this.path = path;
    this.rules = rules;
    this.reportFormat = reportFormat || Format.XML;
    this.reportFile = reportFile || null;
  }

  protected buildCommand(): string {
    // Start with the parts of the command that we know we always want to include.
    let command = `java -cp "${super.buildClasspath().join(':')}" ${HEAP_SIZE} ${MAIN_CLASS}`
      + ` -rulesets ${this.rules} -dir ${this.path} -format ${this.reportFormat}`;

    // Then add anything else that's dynamically included based on other input.
    if (this.reportFile) {
      command += ` -reportfile ${this.reportFile}`;
    }

    // Return the completed command.
    return command;
  }

  /**
   * The callback to handle the results of child_process.exec().
   * @param {Function} res - The 'resolve' method of a Promise.
   * @param {Function} rej - The 'reject' method of a Promise.
   * @override
   */
  protected getCallback(res, rej): PmdSupportCallback {
    return (err, stdout, stderr) => {
      // In addition to the case where err is null, which obviously indicates a successful run, we need to check whether
      // the exit code was 4, which is the status PMD uses for runs that identified rule violations.
      if (err == null || err.code  === 4) {
        // We'll resolve to a tuple containing a boolean that will be true if there were any violations, and stdout.
        // That way, we have a simple indicator of whether there were violations, and a log that we can sweep to know what
        // those violations were.
        res([!!err, stdout]);
      } else {
        // If we got an error with a code other than 4, it means something actually went wrong. We'll just reject with
        // stderr for ease of error handling upstream.
        rej(stderr);
      }
    };
  }
}

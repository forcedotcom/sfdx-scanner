import {Format, PmdSupport} from './PmdSupport';

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

export default class PmdWrapper extends PmdSupport {

  path: string;
  rules: string;
  reportFormat: Format;
  reportFile: string;

  public static async execute(path: string, rules: string, reportFormat: Format, reportFile: string) {
    const myPmd = new PmdWrapper(path, rules, reportFormat, reportFile);
    return myPmd.execute();
  }

  private async execute() {
    return super.runCommand();
  }

  constructor(path: string, rules: string, reportFormat: Format, reportFile: string) {
    super();
    this.path = path;
    this.rules = rules;
    this.reportFormat = reportFormat;
    this.reportFile = reportFile;
  }

  protected buildCommand(): string {
    return `java -cp "${super.buildClasspath().join(':')}" ${HEAP_SIZE} ${MAIN_CLASS}
                -rulesets ${this.rules} -dir ${this.path} -format ${this.reportFormat} -reportfile ${this.reportFile}`;
  }
}

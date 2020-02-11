import {Format, PmdSupport} from './PmdSupport';

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

export default class PmdWrapper extends PmdSupport {

  path: string;
  rules: string;
  reportFormat: Format;
  reportFile: string;
  customRuleJar: string;

  public static async execute(path: string, rules: string, reportFormat: Format, reportFile: string, customRuleJar: string) {
    const myPmd = new PmdWrapper(path, rules, reportFormat, reportFile, customRuleJar);
    return myPmd.execute();
  }

  private async execute() {
    return super.runCommand();
  }

  constructor(path: string, rules: string, reportFormat: Format, reportFile: string, customRuleJar: string = "") {
    super();
    this.path = path;
    this.rules = rules;
    this.reportFormat = reportFormat;
    this.reportFile = reportFile;
    this.customRuleJar = customRuleJar;
  }

  getClassPath(): string[] {
      var classPath = super.buildClasspath();
      if (this.customRuleJar.length > 0) {
          // TODO: verify that jar file exists
        classPath.push(this.customRuleJar);
      }
      return classPath;
  }

  protected buildCommand(): string {
    return `java -cp "${this.getClassPath().join(':')}" ${HEAP_SIZE} ${MAIN_CLASS} -rulesets ${this.rules} -dir ${this.path} -format ${this.reportFormat} -reportfile ${this.reportFile}`;
    // TODO: error handling when an error is thrown for custom rules not working the way it should
  }
}

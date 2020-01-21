import child_process = require('child_process');



/**
 * Output format supported by PMD
 */
export enum Format {
    XML = "xml",
    CSV = "csv",
    TEXT = "txt"
}

export default class PmdWrapper {

    path: string;
    rules: string;
    reportFormat: Format;
    reportFile: string;


    constructor(path: string, rules: string, reportFormat: Format, reportFile: string) {
        this.path = path;
        this.rules = rules;
        this.reportFormat = reportFormat;
        this.reportFile = reportFile;
    }

    public static async execute(path: string, rules: string, reportFormat: Format, reportFile: string) {
        const myPmd = new PmdWrapper(path, rules, reportFormat, reportFile);
        myPmd.runShellCommand();
    }

    async runShellCommand() {
        const runCommand: string = `bash execute-pmd.sh --rulesets ${this.rules} --dir ${this.path} --format ${this.reportFormat} --reportfile ${this.reportFile}`;
        child_process.exec(runCommand, (err, stdout, stderr) => {
            //TODO: revisit this and decide how we want to handle and surface errors
            if (err) {
                console.log(`Error occurred while running pmd: ${err.message}`);
            }
            if (stderr) {
                console.log(`stderr from PMD: ${stderr}`);
            }
            if (stdout) {
                console.log(`Script had something to say: ${stdout}`);
            }
        });
    }
}
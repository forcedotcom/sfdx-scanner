import java = require("java");
import fs = require("fs");
import { PmdDownloader } from "./pmd-downloader";

const pmdClassPath: string = "../../../dist/pmd/lib/*"; // [path.resolve(__dirname, "dist", "pmd", "lib", "*")];
const pmdClassName = "net.sourceforge.pmd.PMD";

/**
 * Output format supported by PMD
 */
export enum Format {
    HTML = "html",
    JSON = "json",
    TEXT = "txt"
}


export default class PMD {
    path: string;
    rules: string;
    reportFormat: Format;


    constructor(path: string, rules: string, reportFormat: Format) {
        this.path = path;
        this.rules = rules;
        this.reportFormat = reportFormat;
    }

    public static execute(path: string, rules: string, reportFormat: Format) {
        const myPmd = new PMD(path, rules, reportFormat);
        myPmd.runPmd();
    }


    runPmd() {
        this._getPmdIfNotAvlb();
        java.classPath.push(pmdClassPath);
        console.log("About to run PMD");
        var returnValue = java.callStaticMethodSync(pmdClassName, "main", "-d", this.path, "-R", this.rules, "-f", this.reportFormat);
        return returnValue;
    }

    _getPmdIfNotAvlb() {
        fs.exists(pmdClassPath, function (exists) {
            if (exists) {
                console.log('PMD already exists.');
            } else {
                console.log('Downloading PMD. . .');
                PmdDownloader.execute();
            }
        });

    }
}
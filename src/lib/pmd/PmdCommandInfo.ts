import {PMD7_LIB, PMD7_VERSION} from "../../Constants";
import * as path from 'path';

const PMD7_CLI_CLASS = 'net.sourceforge.pmd.cli.PmdCli';

export interface PmdCommandInfo {
	getVersion(): string

	getJarPathForLanguage(lang: string): string

	constructJavaCommandArgsForPmd(fileList: string, classPathsForExternalRules: string[], rulesets: string): string[]

	constructJavaCommandArgsForCpd(fileList: string, minimumTokens: number, language: string): string[]
}

export class Pmd7CommandInfo implements PmdCommandInfo {
	getVersion(): string {
		return PMD7_VERSION;
	}

	getJarPathForLanguage(language: string): string {
		return path.join(PMD7_LIB, `pmd-${language}-${this.getVersion()}.jar`);
	}

	constructJavaCommandArgsForPmd(fileList: string, classPathsForExternalRules: string[], rulesets: string): string[] {
		const classpath =  classPathsForExternalRules.concat([`${PMD7_LIB}/*`]).join(path.delimiter);
		const args = ['-cp', classpath, PMD7_CLI_CLASS, 'check', '--file-list', fileList,
			'--format', 'xml'];
		if (rulesets.length > 0) {
			args.push('--rulesets', rulesets);
		}
		return args;
	}

	constructJavaCommandArgsForCpd(fileList: string, minimumTokens: number, language: string): string[] {
		const classpath = `${PMD7_LIB}/*`;
		return ['-cp', classpath, PMD7_CLI_CLASS, 'cpd', '--file-list', fileList, '--format', 'xml',
			'--minimum-tokens', minimumTokens.toString(), '--language', language, '--skip-lexical-errors'];
	}
}

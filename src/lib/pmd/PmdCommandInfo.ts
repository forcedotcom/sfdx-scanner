import {PMD6_LIB, PMD6_VERSION, PMD7_LIB, PMD7_VERSION} from "../../Constants";
import * as path from 'path';

const PMD6_MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const CPD6_MAIN_CLASS = 'net.sourceforge.pmd.cpd.CPD';
const PMD7_CLI_CLASS = 'net.sourceforge.pmd.cli.PmdCli';
const HEAP_SIZE = '-Xmx1024m';

export interface PmdCommandInfo {
	getVersion(): string

	getJarPathForLanguage(lang: string): string

	constructJavaCommandArgsForPmd(fileList: string, classPathsForExternalRules: string[], rulesets: string): string[]

	constructJavaCommandArgsForCpd(fileList: string, minimumTokens: number, language: string): string[]
}

export class Pmd6CommandInfo implements PmdCommandInfo {
	getVersion(): string {
		return PMD6_VERSION;
	}

	getJarPathForLanguage(language: string): string {
		return path.join(PMD6_LIB, `pmd-${language}-${this.getVersion()}.jar`);
	}

	constructJavaCommandArgsForPmd(fileList: string, classPathsForExternalRules: string[], rulesets: string): string[] {
		// The classpath needs PMD's lib folder. There may be redundancy with the shared classpath, but having the
		// same JAR in the classpath twice is fine. Also note that the classpath is not wrapped in quotes like how it
		// would be if we invoked directly through the CLI, because child_process.spawn() hates that.
		const classpath =  classPathsForExternalRules.concat([`${PMD6_LIB}/*`]).join(path.delimiter);
		const args = ['-cp', classpath, HEAP_SIZE, PMD6_MAIN_CLASS, '-filelist', fileList,
			'-format', 'xml'];
		if (rulesets.length > 0) {
			args.push('-rulesets', rulesets);
		}
		return args;
	}

	constructJavaCommandArgsForCpd(fileList: string, minimumTokens: number, language: string): string[] {
		const classpath = `${PMD6_LIB}/*`;
		return ['-cp', classpath, HEAP_SIZE, CPD6_MAIN_CLASS, '--filelist', fileList,
			'--format', 'xml', '--minimum-tokens', minimumTokens.toString(), '--language', language];
	}
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
		const args = ['-cp', classpath, HEAP_SIZE, PMD7_CLI_CLASS, 'check', '--file-list', fileList,
			'--format', 'xml'];
		if (rulesets.length > 0) {
			args.push('--rulesets', rulesets);
		}
		return args;
	}

	constructJavaCommandArgsForCpd(fileList: string, minimumTokens: number, language: string): string[] {
		const classpath = `${PMD7_LIB}/*`;
		const resolvedLanguage = language === 'visualforce' ? 'vf' : language;
		return ['-cp', classpath, HEAP_SIZE, PMD7_CLI_CLASS, 'cpd', '--file-list', fileList, '--format', 'xml',
			'--minimum-tokens', minimumTokens.toString(), '--language', resolvedLanguage, '--skip-lexical-errors'];
	}
}

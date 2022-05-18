import {Controller} from '../../Controller';
import {PmdEngine} from './PmdEngine';
import { CommandLineSupport } from '../services/CommandLineSupport';
import { PMD_LIB } from '../../Constants';

/**
 * Output format supported by PMD
 */
export enum Format {
	XML = 'xml',
	CSV = 'csv',
	TEXT = 'text'
}

export abstract class PmdSupport extends CommandLineSupport {

	protected async buildClasspath(): Promise<string[]> {
		// Include PMD libs into classpath
		const pmdLibs = `${PMD_LIB}/*`;
		const classpathEntries = [pmdLibs];

		// Include custom rule paths into classpath
		const customPathEntries = await this.getCustomRulePathEntries();
		customPathEntries.forEach((pathEntries) => {
			classpathEntries.push(...pathEntries);
		});

		return classpathEntries;
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// PMD's convention is that an exit code of 0 indicates a successful run with no violations, and an exit code of
		// 4 indicates a successful run with at least one violation.
		return code === 0 || code === 4;
	}


	protected abstract buildCommandArray(): Promise<[string, string[]]>;

	protected async getCustomRulePathEntries(): Promise<Map<string, Set<string>>> {
		const customRulePathManager = await Controller.createRulePathManager();
		return customRulePathManager.getRulePathEntries(PmdEngine.ENGINE_NAME);
	}
}

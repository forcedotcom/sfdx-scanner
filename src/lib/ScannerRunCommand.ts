import {Flags} from '@salesforce/sf-plugins-core';
import {ScannerCommand} from './ScannerCommand';
import {BundleName, getMessage} from "../MessageCatalog";
import {OutputFormat} from "./output/OutputFormat";

export abstract class ScannerRunCommand extends ScannerCommand {
	/**
	 * There are flags that are common to all variants of the run command. We can define those flags
	 * here to avoid duplicate code.
	 */
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(BundleName.Common, 'flags.verboseSummary')
		}),
		// BEGIN: Filter-related flags.
		category: Flags.custom<string[]>({
			char: 'c',
			summary: getMessage(BundleName.CommonRun, 'flags.categorySummary'),
			description: getMessage(BundleName.CommonRun, 'flags.categoryDescription'),
			delimiter: ',',
			multiple: true
		})(),
		// BEGIN: Flags related to results processing.
		format: Flags.custom<OutputFormat>({
			char: 'f',
			summary: getMessage(BundleName.CommonRun, 'flags.formatSummary'),
			description: getMessage(BundleName.CommonRun, 'flags.formatDescription'),
			options: Object.values(OutputFormat)
		})(),
		outfile: Flags.string({
			char: 'o',
			summary: getMessage(BundleName.CommonRun, 'flags.outfileSummary'),
			description: getMessage(BundleName.CommonRun, 'flags.outfileDescription')
		}),
		'severity-threshold': Flags.integer({
			char: 's',
			summary: getMessage(BundleName.CommonRun, 'flags.sevthresholdSummary'),
			description: getMessage(BundleName.CommonRun, 'flags.sevthresholdDescription'),
			exclusive: ['json'],
			min: 1,
			max: 3
		}),
		'normalize-severity': Flags.boolean({
			summary: getMessage(BundleName.CommonRun, 'flags.normalizesevSummary'),
			description: getMessage(BundleName.CommonRun, 'flags.normalizesevDescription')
		}),
		// END: Flags related to results processing.
		// BEGIN: Flags related to targeting.
		projectdir: Flags.custom<string[]>({
			char: 'p',
			summary: getMessage(BundleName.CommonRun, 'flags.projectdirSummary'),
			description: getMessage(BundleName.CommonRun, 'flags.projectdirDescription'),
			delimiter: ',',
			multiple: true
		})(),
		// END: Flags related to targeting.
	};
}

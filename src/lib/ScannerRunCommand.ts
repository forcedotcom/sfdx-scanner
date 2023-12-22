import {Flags} from '@salesforce/sf-plugins-core';
import {ScannerCommand} from './ScannerCommand';
import {OUTPUT_FORMAT} from './RuleManager';
import untildify = require('untildify');
import normalize = require('normalize-path');
import {Bundle, getMessage} from "../MessageCatalog";

export abstract class ScannerRunCommand extends ScannerCommand {
	/**
	 * There are flags that are common to all variants of the run command. We can define those flags
	 * here to avoid duplicate code.
	 */
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getMessage(Bundle.Common, 'flags.verboseSummary')
		}),
		// BEGIN: Filter-related flags.
		category: Flags.custom<string[]>({
			char: 'c',
			summary: getMessage(Bundle.CommonRun, 'flags.categorySummary'),
			description: getMessage(Bundle.CommonRun, 'flags.categoryDescription'),
			delimiter: ',',
			multiple: true
		})(),
		// BEGIN: Flags related to results processing.
		format: Flags.custom<OUTPUT_FORMAT>({
			char: 'f',
			summary: getMessage(Bundle.CommonRun, 'flags.formatSummary'),
			description: getMessage(Bundle.CommonRun, 'flags.formatDescription'),
			options: Object.values(OUTPUT_FORMAT)
		})(),
		outfile: Flags.string({
			char: 'o',
			summary: getMessage(Bundle.CommonRun, 'flags.outfileSummary'),
			description: getMessage(Bundle.CommonRun, 'flags.outfileDescription')
		}),
		'severity-threshold': Flags.integer({
			char: 's',
			summary: getMessage(Bundle.CommonRun, 'flags.sevthresholdSummary'),
			description: getMessage(Bundle.CommonRun, 'flags.sevthresholdDescription'),
			exclusive: ['json'],
			min: 1,
			max: 3
		}),
		'normalize-severity': Flags.boolean({
			summary: getMessage(Bundle.CommonRun, 'flags.normalizesevSummary'),
			description: getMessage(Bundle.CommonRun, 'flags.normalizesevDescription')
		}),
		// END: Flags related to results processing.
		// BEGIN: Flags related to targeting.
		projectdir: Flags.custom<string[]>({ // TODO: FIGURE OUT WHY WE NEED THIS ON BOTH "run" AND "run dfa"
			char: 'p',
			summary: getMessage(Bundle.CommonRun, 'flags.projectdirSummary'),
			description: getMessage(Bundle.CommonRun, 'flags.projectdirDescription'),
			parse: val => Promise.resolve(val.split(',').map(d => normalize(untildify(d))))
		})(),
		// END: Flags related to targeting.
	};
}

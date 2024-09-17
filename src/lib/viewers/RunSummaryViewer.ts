import {CodeAnalyzerConfig, RunResults, SeverityLevel} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {indent, toStyledHeader} from '../utils/StylingUtil';
import {BundleName, getMessage} from '../messages';

export interface RunSummaryViewer {
	view(results: RunResults, config: CodeAnalyzerConfig, outfiles: string[]): void;
}

export class RunSummaryDisplayer {
	protected display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(results: RunResults, config: CodeAnalyzerConfig, outfiles: string[]): void {
		this.display.displayLog(toStyledHeader(getMessage(BundleName.RunSummaryViewer, 'summary.header')));
		// Use empty line as a visual separator
		this.display.displayLog('');

		if (results.getViolationCount() === 0) {
			this.display.displayLog(getMessage(BundleName.RunSummaryViewer, 'summary.found-no-violations'));
		} else {
			this.displayResultsSummary(results);
		}
		// Use empty line as a visual separator
		this.display.displayLog('');

		if (outfiles.length === 0) {
			this.display.displayLog(getMessage(BundleName.RunSummaryViewer, 'summary.no-outfiles'));
		} else {
			this.displayOutfiles(outfiles);
		}
		// Use empty line as a visual separator
		this.display.displayLog('');

		this.display.displayLog(getMessage(BundleName.RunSummaryViewer, 'summary.log-file-location'));
		this.display.displayLog(indent(config.getLogFolder()));
	}

	private displayResultsSummary(results: RunResults): void {
		this.display.displayLog(getMessage(BundleName.RunSummaryViewer, 'summary.violations-total', [results.getViolationCount()]));
		for (const sev of Object.values(SeverityLevel)) {
			// Some of the keys will be numbers, since the enum is numerical. Skip those.
			if (typeof sev !== 'string') {
				continue;
			}
			const sevCount = results.getViolationCountOfSeverity(SeverityLevel[sev] as SeverityLevel);
			if (sevCount > 0) {
				this.display.displayLog(indent(getMessage(BundleName.RunSummaryViewer, 'summary.violations-item', [sevCount, sev])));
			}
		}
	}

	private displayOutfiles(outfiles: string[]): void {
		this.display.displayLog(getMessage(BundleName.RunSummaryViewer, 'summary.outfiles-total'));
		for (const outfile of outfiles) {
			this.display.displayLog(indent(outfile));
		}
	}
}

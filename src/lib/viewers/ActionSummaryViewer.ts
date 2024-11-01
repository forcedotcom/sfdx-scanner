import {Display} from '../Display';
import {RuleSelection} from '@salesforce/code-analyzer-core';
import {toStyledHeader, indent} from '../utils/StylingUtil';
import {BundleName, getMessage} from '../messages';

abstract class AbstractActionSummaryViewer {
	protected readonly display: Display;

	protected constructor(display: Display) {
		this.display = display;
	}

	protected displaySummaryHeader(): void {
		this.display.displayLog(toStyledHeader(getMessage(BundleName.ActionSummaryViewer, 'common.summary-header')));
	}

	protected displayLineSeparator(): void {
		this.display.displayLog("");
	}

	protected displayLogFileInfo(logFile: string): void {
		this.display.displayLog(getMessage(BundleName.ActionSummaryViewer, 'common.logfile-location'));
		this.display.displayLog(indent(logFile));
	}
}

export class ConfigActionSummaryViewer extends AbstractActionSummaryViewer {
	public constructor(display: Display) {
		super(display);
	}

	public view(logFile: string, outfile?: string): void {
		this.displaySummaryHeader();
		this.displayLineSeparator();

		if (outfile) {
			this.displayOutfile(outfile);
			this.displayLineSeparator();
		}

		this.displayLogFileInfo(logFile);
	}

	private displayOutfile(outfile: string): void {
		this.display.displayLog(getMessage(BundleName.ActionSummaryViewer, 'config-action.outfile-location'));
		this.display.displayLog(indent(outfile));
	}
}

export class RulesActionSummaryViewer extends AbstractActionSummaryViewer {
	public constructor(display: Display) {
		super(display);
	}

	public view(ruleSelection: RuleSelection, logFile: string): void {
		// Start with separator to cleanly break from anything that's already been logged.
		this.displayLineSeparator();
		this.displaySummaryHeader();
		this.displayLineSeparator();

		if (ruleSelection.getCount() === 0) {
			this.display.displayLog(getMessage(BundleName.ActionSummaryViewer, 'rules-action.found-no-rules'));
		} else {
			this.displayRuleSelection(ruleSelection);
		}
		this.displayLineSeparator();

		this.displayLogFileInfo(logFile);
	}

	private displayRuleSelection(ruleSelection: RuleSelection): void {
		this.display.displayLog(getMessage(BundleName.ActionSummaryViewer, 'rules-action.rules-total', [ruleSelection.getCount(), ruleSelection.getEngineNames().length]));
		for (const engineName of ruleSelection.getEngineNames()) {
			const ruleCountForEngine: number = ruleSelection.getRulesFor(engineName).length;
			this.display.displayLog(indent(getMessage(BundleName.ActionSummaryViewer, 'rules-action.rules-item', [ruleCountForEngine, engineName])));
		}
	}


}

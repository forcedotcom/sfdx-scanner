import {Ux} from '@salesforce/sf-plugins-core';
import {RunResults, SeverityLevel, Violation} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {BundleName, getMessage} from '../messages';

export interface ResultsViewer {
	view(results: RunResults): void;
}

abstract class AbstractResultsViewer implements ResultsViewer {
	protected display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(results: RunResults): void {
		if (results.getViolationCount() === 0) {
			this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.found-no-results'));
		} else {
			this.displaySummary(results.getViolations());
			this._view(results);
		}
	}

	private displaySummary(violations: Violation[]): void {
		const violationCount = violations.length;
		const fileCount = this.countUniqueFiles(violations);
		this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.found-results', [violationCount, fileCount]));
	}

	private countUniqueFiles(violations: Violation[]): number {
		const fileSet: Set<string> = new Set();
		violations.forEach(v => {
			const primaryLocation = v.getCodeLocations()[v.getPrimaryLocationIndex()];
			const file = primaryLocation.getFile();
			if (file) {
				fileSet.add(file);
			}
		});
		return fileSet.size;
	}

	protected abstract _view(results: RunResults): void;
}

export class ResultsDetailViewer extends AbstractResultsViewer {
	/**
	 * The display format consumes a lot of vertical space. As such, we're choosing to limit the total
	 * number of violations that it will display before truncating the results and instructing the user
	 * to consult one of the output files.
	 */
	public static readonly RESULTS_CUTOFF = 10;

	protected _view(results: RunResults): void {
		const violations = sortViolations(results.getViolations());

		this.displayDetails(violations);
		this.displayBreakdown(results);
	}

	private displayDetails(violations: Violation[]): void {
		// If there are more results than the cutoff amount, we'll only show that many.
		const printableViolationCount = Math.min(ResultsDetailViewer.RESULTS_CUTOFF, violations.length);
		const omittedResultsCount = violations.length - printableViolationCount;
		for (let i = 0; i < printableViolationCount; i++) {
			const violation = violations[i];
			const rule = violation.getRule();
			const sev = rule.getSeverityLevel();
			this.display.displayStyledHeader(
				getMessage(
					BundleName.ResultsViewer,
					'summary.detail.violation-header',
					[i + 1, rule.getName()])
			);
			const primaryLocation = violation.getCodeLocations()[violation.getPrimaryLocationIndex()];
			this.display.displayStyledObject({
				severity: `${sev.valueOf()} (${SeverityLevel[sev]})`,
				engine: rule.getEngineName(),
				message: violation.getMessage(),
				location: `${primaryLocation.getFile()}:${primaryLocation.getStartLine()}:${primaryLocation.getStartColumn()}`,
				resources: violation.getResourceUrls().join(',')
			}, ['severity', 'engine', 'message', 'location', 'resources']);
		}
		if (omittedResultsCount > 0) {
			this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.detail.results-truncated', [omittedResultsCount]));
		}
	}

	private displayBreakdown(results: RunResults): void {
		this.display.displayStyledHeader(getMessage(BundleName.ResultsViewer, 'summary.detail.breakdown.header'));
		this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.detail.breakdown.total', [results.getViolationCount()]));
		for (const sev of Object.values(SeverityLevel)) {
			// Some of the keys will be numbers, since the enum is numerical. Skip those.
			if (typeof sev !== 'string') {
				continue;
			}
			const sevCount = results.getViolationCountOfSeverity(SeverityLevel[sev] as SeverityLevel);
			if (sevCount > 0) {
				this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.detail.breakdown.item', [sevCount, sev]));
			}
		}
	}
}

type ResultRow = {
	num: number;
	location: string;
	rule: string;
	severity: string;
	message: string;
}

const TABLE_COLUMNS: Ux.Table.Columns<ResultRow> = {
	num: {
		header: getMessage(BundleName.ResultsViewer, 'summary.table.num-column'),
	},
	severity: {
		header: getMessage(BundleName.ResultsViewer, 'summary.table.severity-column')
	},
	rule: {
		header: getMessage(BundleName.ResultsViewer, 'summary.table.rule-column')
	},
	location: {
		header: getMessage(BundleName.ResultsViewer, 'summary.table.location-column')
	},
	message: {
		header: getMessage(BundleName.ResultsViewer, 'summary.table.message-column')
	}
};

export class ResultsTableViewer extends AbstractResultsViewer {
	protected _view(results: RunResults) {
		const resultRows: ResultRow[] = sortViolations(results.getViolations())
			.map((v, idx) => {
				const severity = v.getRule().getSeverityLevel();
				const primaryLocation = v.getCodeLocations()[v.getPrimaryLocationIndex()];
				return {
					num: idx + 1,
					location: `${primaryLocation.getFile()}:${primaryLocation.getStartLine()}:${primaryLocation.getStartColumn()}`,
					rule: `${v.getRule().getEngineName()}:${v.getRule().getName()}`,
					severity: `${severity.valueOf()} (${SeverityLevel[severity]})`,
					message: v.getMessage()
				}
			});
		this.display.displayTable(resultRows, TABLE_COLUMNS);
	}
}

function sortViolations(violations: Violation[]): Violation[] {
	return violations.toSorted((v1, v2) => {
		// First compare severities.
		const v1Sev = v1.getRule().getSeverityLevel();
		const v2Sev = v2.getRule().getSeverityLevel();
		if (v1Sev !== v2Sev) {
			return v1Sev - v2Sev;
		}
		// Next, compare file names.
		const v1PrimaryLocation = v1.getCodeLocations()[v1.getPrimaryLocationIndex()];
		const v2PrimaryLocation = v2.getCodeLocations()[v2.getPrimaryLocationIndex()];
		const v1File = v1PrimaryLocation.getFile() || '';
		const v2File = v2PrimaryLocation.getFile() || '';
		if (v1File !== v2File) {
			return v1File.localeCompare(v2File);
		}

		// Next, compare start lines.
		const v1StartLine = v1PrimaryLocation.getStartLine() || 0;
		const v2StartLine = v2PrimaryLocation.getStartLine() || 0;
		if (v1StartLine !== v2StartLine) {
			return v1StartLine - v2StartLine;
		}

		// Next, compare start columns.
		const v1StartColumn = v1PrimaryLocation.getStartColumn() || 0;
		const v2StartColumn = v2PrimaryLocation.getStartColumn() || 0;
		return v1StartColumn - v2StartColumn;
	});
}

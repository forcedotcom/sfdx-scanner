import {Ux} from '@salesforce/sf-plugins-core';
import {CodeLocation, RunResults, SeverityLevel, Violation} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {toStyledHeaderAndBody} from '../utils/StylingUtil';
import {BundleName, getMessage} from '../messages';
import path from "node:path";

export interface ResultsViewer {
	view(results: RunResults): void;
}

abstract class AbstractResultsDisplayer implements ResultsViewer {
	protected display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public view(results: RunResults): void {
		if (results.getViolationCount() === 0) {
			return;
		} else {
			this._view(results);
			this.display.displayLog('\n');
		}
	}

	protected countUniqueFiles(violations: Violation[]): number {
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

export class ResultsDetailDisplayer extends AbstractResultsDisplayer {
	protected _view(results: RunResults): void {
		const violations = sortViolations(results.getViolations());

		this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.detail.found-results', [
			violations.length, this.countUniqueFiles(violations)]));
		this.displayDetails(violations);
	}

	private displayDetails(violations: Violation[]): void {
		const styledViolations: string[] = violations
			.map((violation, idx) => this.styleViolation(violation, idx));
		this.display.displayLog(styledViolations.join('\n\n'));
	}

	private styleViolation(violation: Violation, idx: number): string {
		const rule = violation.getRule();
		const sev = rule.getSeverityLevel();
		const primaryLocation = violation.getCodeLocations()[violation.getPrimaryLocationIndex()];

		const header = getMessage(
			BundleName.ResultsViewer,
			'summary.detail.violation-header',
			[idx + 1, rule.getName()]
		);
		const body = {
			severity: `${sev.valueOf()} (${SeverityLevel[sev]})`,
			engine: rule.getEngineName(),
			message: violation.getMessage(),
			location: `${primaryLocation.getFile()}:${primaryLocation.getStartLine()}:${primaryLocation.getStartColumn()}`,
			resources: violation.getResourceUrls().join(',')
		};
		const keys = ['severity', 'engine', 'message', 'location', 'resources'];
		return toStyledHeaderAndBody(header, body, keys);
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

export class ResultsTableDisplayer extends AbstractResultsDisplayer {
	protected _view(results: RunResults) {
		const violations: Violation[] = sortViolations(results.getViolations());
		const parentFolder: string = findLongestCommonParentFolderOf(violations.map(v =>
			getPrimaryLocation(v).getFile()).filter(f => f !== undefined));

		const resultRows: ResultRow[] = violations.map((v, idx) => {
				const severity = v.getRule().getSeverityLevel();
				const primaryLocation = getPrimaryLocation(v);
				const relativeFile: string | null = primaryLocation.getFile() ?
					path.relative(parentFolder, primaryLocation.getFile()!) : null;
				return {
					num: idx + 1,
					location: `${relativeFile}:${primaryLocation.getStartLine()}:${primaryLocation.getStartColumn()}`,
					rule: `${v.getRule().getEngineName()}:${v.getRule().getName()}`,
					severity: `${severity.valueOf()} (${SeverityLevel[severity]})`,
					message: v.getMessage()
				}
			});

		this.display.displayLog(getMessage(BundleName.ResultsViewer, 'summary.table.found-results', [
			violations.length, this.countUniqueFiles(violations), parentFolder]));
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

// TODO: We should update core module to have this function directly on the Violation object and then remove this helper
function getPrimaryLocation(violation: Violation): CodeLocation {
	return violation.getCodeLocations()[violation.getPrimaryLocationIndex()];
}

/**
 * Returns the longest comment parent folder of the file paths provided
 * Note that this function assumes that all the paths are indeed files and not folders
 */
export function findLongestCommonParentFolderOf(filePaths: string[]): string {
	const roots: string[] = filePaths.map(filePath => path.parse(filePath).root);
	const commonRoot: string = (new Set(roots)).size === 1 ? roots[0] : '';
	if (!commonRoot) {
		return '';
	}
	const commonFolders: string[] = [];
	let depth: number = 0;
	const explodedPaths: string[][] = filePaths.map(file =>
		path.dirname(file).slice(commonRoot.length).split(path.sep).filter(v => v.length > 0));
	while(explodedPaths.every(folders => folders.length > depth)) {
		const currFolder: string = explodedPaths[0][depth];
		if (explodedPaths.some(folders => folders[depth] !== currFolder)) {
			break;
		}
		commonFolders.push(currFolder);
		depth++;
	}
	return path.join(commonRoot, ... commonFolders);
}

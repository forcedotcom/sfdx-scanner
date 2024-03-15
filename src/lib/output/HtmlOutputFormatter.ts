import {FormattedOutput, RuleResult} from "../../types";
import {OutputFormatter, Results} from "./Results";
import {isPathlessViolation} from "../util/Utils";
import {ENGINE} from "../../Constants";
import {FileHandler} from "../util/FileHandler";
import * as path from 'path';
import * as Mustache from "mustache";

export class HtmlOutputFormatter implements OutputFormatter {
	private readonly verboseViolations: boolean;

	public constructor(verboseViolations: boolean) {
		this.verboseViolations = verboseViolations;
	}

	public async format(results: Results): Promise<FormattedOutput> {
		const isDfa = results.violationsAreDfa();
		const ruleResults: RuleResult[] = results.getRuleResults();
		const normalizeSeverity: boolean = ruleResults[0]?.violations.length > 0 && !(ruleResults[0]?.violations[0].normalizedSeverity === undefined);

		const violations = [];
		for (const result of ruleResults) {
			for (const v of result.violations) {
				let htmlFormattedViolation;
				if (isPathlessViolation(v)) {
					htmlFormattedViolation = {
						engine: result.engine,
						fileName: result.fileName,
						severity: (normalizeSeverity ? v.normalizedSeverity : v.severity),
						ruleName: v.ruleName,
						category: v.category,
						url: v.url,
						message: this.verboseViolations && (result.engine as ENGINE) === ENGINE.RETIRE_JS ? v.message.replace(/\n/g, '<br>') : v.message, // <br> used for line breaks in html
						line: v.line,
						column: v.column,
						endLine: v.endLine || null,
						endColumn: v.endColumn || null
					}
				} else {
					htmlFormattedViolation = {
						engine: result.engine,
						fileName: result.fileName,
						severity: (normalizeSeverity ? v.normalizedSeverity : v.severity),
						ruleName: v.ruleName,
						category: v.category,
						url: v.url,
						message: v.message,
						line: v.sourceLine,
						column: v.sourceColumn,
						sinkFileName: v.sinkFileName,
						sinkLine: v.sinkLine,
						sinkColumn: v.sinkColumn
					};
				}
				violations.push(htmlFormattedViolation);
			}
		}

		// Populate the template with a JSON payload of the violations
		const fileHandler = new FileHandler();
		const templateName = isDfa ? 'dfa-simple.mustache' : 'simple.mustache';
		const template = await fileHandler.readFile(path.resolve(__dirname, '..', '..', '..', 'html-templates', templateName));
		// Quirk: This should work fine in production, but in development it will lead to `dev` or `run`.
		const exeName = path.parse(process.argv[1]).name;
		const args = [exeName];
		let parsingCommand = true;
		for (const arg of process.argv.slice(2)) {
			// If the argument starts with a `-`, then it's a flag.
			if (arg.startsWith('-')) {
				// Note that we're done parsing the command itself.
				parsingCommand = false;
				// Pass flags as-is instead of wrapping in quotes.
				args.push(arg);
			} else if (parsingCommand) {
				// If we're still parsing the command, then the arg gets passed in as-is instead
				// of being wrapped in quotes.
				args.push(arg);
			} else {
				// If we're not parsing a flag or the command, then we're parsing a flag parameter.
				// Wrap it in quotes.
				args.push(`"${arg}"`);
			}
		}
		const templateData = {
			violations: JSON.stringify(violations),
			workingDirectory: process.cwd(),
			commandLine: args.join(' ')
		};

		return Mustache.render(template, templateData);
	}
}

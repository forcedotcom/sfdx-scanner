import {OutputFormatter, RunResults} from "./Results";
import {FormattedOutput, Rule, RuleResult, RuleViolation} from "../../types";
import {Location, Log, Notification, Region, ReportingDescriptor, Result, Run} from "sarif";
import {deepCopy, isPathlessViolation} from "../util/Utils";
import * as url from "url";
import {RuleCatalog} from "../services/RuleCatalog";
import {ESLint} from "eslint";
import {ENGINE, PMD_VERSION, SFGE_VERSION} from "../../Constants";
import {Controller} from "../../Controller";
import * as retire from 'retire';

const ERROR = 'error';
const WARNING = 'warning';
const SINK_VERTEX_MESSAGE = "Sink vertex found here.";

export class SarifOutputFormatter implements OutputFormatter {

	/**
	 * Convert an array of RuleResults to a sarif document. The rules are separated by engine name.
	 * A new "run" object is created for each engine that was run
	 */
	public async format(results: RunResults): Promise<FormattedOutput> {
		// Obtain the catalog and pass it in, this avoids multiple initializations
		// when waiting for promises in parallel
		const catalog: RuleCatalog = await Controller.getCatalog();
		const sarif: Log = {
			version: '2.1.0',
			$schema: 'http://json.schemastore.org/sarif-2.1.0',
			runs: [
			]
		};

		// Partition the RuleResults by the engine that generated them. Certain engines may
		// have multiple RuleResults depending on how the targets were provided to the run command.
		// Some engines may have no results, these engines should still generate a "run" node
		const filteredResults: Map<string, RuleResult[]> = new Map<string, RuleResult[]>();
		for (const engine of results.getExecutedEngines()) {
			filteredResults.set(engine, []);
		}
		for (const r of results.getRuleResults()) {
			filteredResults.get(r.engine).push(r);
		}

		// Create a new run object for each engine/results pair
		for (const [engine, ruleResults] of filteredResults.entries()) {
			const formatter: SarifFormatter = getSarifFormatter(engine);
			sarif.runs.push(formatter.format(catalog, ruleResults));
		}

		return JSON.stringify(sarif);
	}
}

/**
 * Formatter based on https://docs.oasis-open.org/sarif/sarif/v2.0/csprd02/sarif-v2.0-csprd02.html
 *
 * The sarif format contains an array of "run" objects. There is a one to one mapping between a run
 * and an engine. For example, an invocation that executes the eslint and pmd engines will have two
 * run objects.
 */
abstract class SarifFormatter {
	/**
	 * Each run contains an array of rules that ran. The violations refer to these rules by their
	 * id and their index. This map keeps track of the index for a given rule.
	 */
	private readonly ruleMap: Map<string, number>;
	/**
	 * The initial json object that contains basic information such as the engine name.
	 * This object will be added to as the violations are processed.
	 */
	private readonly jsonTemplate: Run;
	protected readonly engine: string;

	protected constructor(engine: string, runJson: Run) {
		this.engine = engine;
		this.jsonTemplate = deepCopy(runJson);
		this.jsonTemplate['results'] = [];
		this.jsonTemplate['invocations'] = [];
		this.ruleMap = new Map<string, number>();
	}

	/**
	 * Converts the rule violation to either "warning" or "error"
	 * See https://docs.oasis-open.org/sarif/sarif/v2.0/csprd02/sarif-v2.0-csprd02.html#_Toc10127839
	 */
	protected abstract getLevel(violation: RuleViolation): Notification.level;

	/**
	 * Return a location that is used by violations and errors
	 */
	private static getLocation(fileName: string): Location {
		return {
			physicalLocation: {
				artifactLocation: {
					uri: url.pathToFileURL(fileName).toString()
				}
			}
		};
	}

	/**
	 * Find all unique rules and add them to ruleMap
	 * @return the array that is appended to tool.driver.rules for the given run
	 */
	private populateRuleMap(catalog: RuleCatalog, ruleResults: RuleResult[]): ReportingDescriptor[] {
		const rules: ReportingDescriptor[] = [];
		const normalizeSeverity: boolean = ruleResults.length > 0 && ruleResults[0].violations.length > 0 && !(ruleResults[0].violations[0].normalizedSeverity === undefined);
		for (const r of ruleResults) {
			for (const v of r.violations) {
				// Exceptions aren't tied to a rule
				if (v.exception) {
					continue;
				}
				if (!this.ruleMap.has(v.ruleName)) {
					const vRule: Rule = catalog.getRule(r.engine, v.ruleName);

					// v.Rule may be undefined if there was an error
					const description: string = vRule?.description ? vRule.description : v.message;
					this.ruleMap.set(v.ruleName, this.ruleMap.size);
					const rule = {
						id: v.ruleName,
						shortDescription: {
							// Replace newline at beginning and end of line
							text: description.trim().replace(/^\n/, '').replace(/\n$/, '')
						},
						properties: {
							category: v.category,
							severity: v.severity,
							normalizedSeverity: (normalizeSeverity ? v.normalizedSeverity : undefined) // when set to undefined, normalizedSeverity will not appear in output
						}
					};
					if (v.url) {
						rule['helpUri'] = v.url;
					}
					rules.push(rule);
				}
			}
		}
		return rules;
	}

	/**
	 * Convert an array of RuleResults to a run object. A given engine may create multiple RuleResult
	 * objects for a given invocation, this typically happens if multiple targets are provided.
	 * Multiple RuleResult objects will be consolidated into a single run object.
	 * https://docs.oasis-open.org/sarif/sarif/v2.0/csprd02/sarif-v2.0-csprd02.html#_Toc10127675
	 */
	public format(catalog: RuleCatalog, ruleResults: RuleResult[]): Run {
		const runJson = deepCopy(this.jsonTemplate);
		const invocation = {
			executionSuccessful: true,
			toolExecutionNotifications: []
		}

		runJson.tool.driver.rules.push(...this.populateRuleMap(catalog, ruleResults));

		for (const r of ruleResults) {
			for (const v of r.violations) {
				// Create a new location for each violation, it may contain region specific information
				const location = SarifFormatter.getLocation(r.fileName);

				// Violations that are exceptions are placed in the invocation.toolExecutionNotifications array
				if (v.exception) {
					invocation.executionSuccessful = false;
					invocation.toolExecutionNotifications.push({
						locations: [location],
						message: {
							text: v.message.replace(/\n/g, '')
						}
					});
				} else {
					// Regular violations are placed in the results array.
					// Start with a basic result, and we'll fill in the other attributes gradually.
					const result: Result = {
						level: this.getLevel(v),
						ruleId: v.ruleName,
						ruleIndex: this.ruleMap.get(v.ruleName),
						message: {
							text: v.message.replace(/\n/g, '')
						}
					};
					// The location-based attributes depend on whether the violation is for a pathless or DFA rule.
					const isPathless = isPathlessViolation(v);
					// W-8881776: line and column are sometimes strings, but the schema requires them to be numbers
					const region: Region = {
						startLine: parseInt(`${isPathless ? v.line : v.sourceLine}`),
						startColumn: parseInt(`${isPathless ? v.column : v.sourceColumn}`)
					};

					if (isPathless && v.endLine != null) {
						region.endLine = parseInt(`${v.endLine}`);
					}
					if (isPathless && v.endColumn != null) {
						region.endColumn = parseInt(`${v.endColumn}`);
					}

					location.physicalLocation.region = region;
					result.locations = [location];

					// If the violation is DFA, we'll want to create a secondary location for the sink vertex.
					if (!isPathless && v.sinkFileName) {
						const secondaryLocation = SarifFormatter.getLocation(v.sinkFileName);
						secondaryLocation.message = {
							text: SINK_VERTEX_MESSAGE
						};
						secondaryLocation.physicalLocation.region = {
							startLine: parseInt(`${v.sinkLine}`),
							startColumn: parseInt(`${v.sinkColumn}`)
						};
						result.relatedLocations = [secondaryLocation];
					}

					runJson.results.push(result);
				}
			}
		}

		runJson['invocations'].push(invocation);

		return runJson;
	}
}

/**
 * Generates a run object for all eslint based engines.
 * The tool.driver.name will be the specific engine that ran.
 */
class ESLintSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: engine,
						version: ESLint.version,
						informationUri: 'https://eslint.org',
						rules: []
					}
				}
			}
		);
	}

	protected getLevel(ruleViolation: RuleViolation): Notification.level {
		return ruleViolation.severity === 2 ? ERROR : WARNING;
	}
}

/**
 * Generates a run object for all pmd based engines.
 */
class PMDSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: ENGINE.PMD,
						version: PMD_VERSION,
						informationUri: 'https://pmd.github.io/pmd',
						rules: []
					}
				}
			}
		);
	}

	protected getLevel(ruleViolation: RuleViolation): Notification.level {
		return ruleViolation.severity === 1 ? ERROR : WARNING;
	}
}


class CPDSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: ENGINE.CPD,
						version: PMD_VERSION, /*CPD would use the same PMD version*/
						informationUri: 'https://pmd.github.io/latest/pmd_userdocs_cpd.html',
						rules: []
					}
				}
			}
		);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	protected getLevel(ignored: RuleViolation): Notification.level {
		return WARNING;
	}
}

/**
 * Generates a run object for retire-js
 */
class RetireJsSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: 'Retire.js',
						// eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
						version: retire.version as string,
						informationUri: 'https://retirejs.github.io/retire.js',
						rules: []
					}
				}
			}
		);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	protected getLevel(ignored: RuleViolation): Notification.level {
		// All violations are errors
		return ERROR;
	}
}


class SfgeSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine, {
			tool: {
				driver: {
					name: ENGINE.SFGE,
					version: SFGE_VERSION,
					informationUri: 'https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/introduction/',
					rules: []
				}
			}
		});
	}

	protected getLevel(ruleViolation: RuleViolation): Notification.level {
		return ruleViolation.severity === 1 ? ERROR : WARNING;
	}
}

const getSarifFormatter = (engine: string): SarifFormatter => {
	if (engine === ENGINE.ESLINT_CUSTOM) {
		// Expose the eslint-custom engine as eslint, the users don't need to know it
		// was the custom implementation
		return new ESLintSarifFormatter(ENGINE.ESLINT);
	} else if (engine.startsWith(ENGINE.ESLINT)) {
		// All other eslint engines are exposed as-is
		return new ESLintSarifFormatter(engine);
	} else if (engine.startsWith(ENGINE.PMD)) {
		// Use the same formatter for pmd and pmd-custom, the users don't need to know it
		// was the custom implementation
		return new PMDSarifFormatter(ENGINE.PMD);
	} else if (engine === ENGINE.RETIRE_JS) {
		return new RetireJsSarifFormatter(engine);
	} else if (engine === ENGINE.CPD) {
		return new CPDSarifFormatter(engine);
	} else if (engine === ENGINE.SFGE) {
		return new SfgeSarifFormatter(engine);
	} else {
		throw new Error(`Developer error. Unknown engine '${engine}'`);
	}
}

import { deepCopy } from '../util/Utils';
import { ENGINE, PMD_VERSION } from '../../Constants';
import { Rule, RuleResult, RuleViolation } from '../../types';
import * as url from 'url';
import { RuleCatalog } from '../services/RuleCatalog';
import { Controller } from '../../Controller';
import { CLIEngine } from 'eslint';
import * as retire from 'retire';

const ERROR = 'error';
const WARNING = 'warning';

/**
 * Formatter based on https://docs.oasis-open.org/sarif/sarif/v2.0/csprd02/sarif-v2.0-csprd02.html
 */
abstract class SarifFormatter {
	private readonly ruleMap: Map<string, number>;
	private ruleIndex: number;
	private readonly jsonTemplate: unknown;
	protected readonly engine: string;

	constructor(engine: string, toolJson: unknown) {
		this.engine = engine;
		this.jsonTemplate = deepCopy(toolJson);
		this.jsonTemplate['results'] = [];
		this.jsonTemplate['invocations'] = [];
		this.ruleIndex = 0;
		this.ruleMap = new Map<string, number>();
	}

	protected abstract getLevel(violation: RuleViolation): string;

	private getLocation(r: RuleResult): unknown {
		return {
			physicalLocation: {
				artifactLocation: {
					uri: url.pathToFileURL(r.fileName)
				}
			}
		};
	}

	private populateRuleMap(catalog: RuleCatalog, ruleResults: RuleResult[]): unknown[] {
		const rules = [];
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
					this.ruleMap.set(v.ruleName, this.ruleIndex++);
					const rule = {
						id: v.ruleName,
						shortDescription: {
							// Replace newline at beginning and end of line
							text: description.trim().replace(/^\n/, '').replace(/\n$/, '')
						},
						properties: {
							category: v.category,
							severity: v.severity
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

	public format(catalog: RuleCatalog, ruleResults: RuleResult[]): unknown {
		const runJson = deepCopy(this.jsonTemplate);
		const results = runJson.results;
		const invocation = {
			executionSuccessful: true,
			toolExecutionNotifications: []
		}

		runJson.tool.driver.rules.push(...this.populateRuleMap(catalog, ruleResults));

		for (const r of ruleResults) {
			for (const v of r.violations) {
				// Create a new location for each violation, it may contain region specific information
				const location = this.getLocation(r);

				if (v.exception) {
					invocation.executionSuccessful = false;
					invocation.toolExecutionNotifications.push({
						locations: [location],
						message: {
							text: v.message.replace(/\n/g, '')
						}
					});
				} else {
					// W-8881776: line and column are sometimes strings, but the schema requires them to be numbers
					const region = {
						startLine: parseInt(`${v.line}`),
						startColumn: parseInt(`${v.column}`)
					};

					for (const k of ['endLine', 'endColumn']) {
						if (v[k]) {
							region[k] = parseInt(v[k]);
						}
					}

					location['physicalLocation']['region'] = region;

					const result = {
						level: this.getLevel(v),
						ruleId: v.ruleName,
						ruleIndex: this.ruleMap.get(v.ruleName),
						message: {
							text: v.message.replace(/\n/g, '')
						},
						locations: [location]
					};

					results.push(result);
				}
			}
		}

		runJson['invocations'].push(invocation);

		return runJson;
	}
}

class ESLintSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: engine,
						version: CLIEngine.version,
						informationUri: 'https://eslint.org',
						rules: []
					}
				}
			}
		);
	}

	protected getLevel(ruleViolation: RuleViolation): string {
		return ruleViolation.severity === 2 ? ERROR : WARNING;
	}
}

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

	protected getLevel(ruleViolation: RuleViolation): string {
		return ruleViolation.severity === 1 ? ERROR : WARNING;
	}
}

class RetireJsSarifFormatter extends SarifFormatter {
	constructor(engine: string) {
		super(engine,
			{
				tool: {
					driver: {
						name: 'Retire.js',
						version: retire.version,
						informationUri: 'https://retirejs.github.io/retire.js',
						rules: []
					}
				}
			}
		);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	protected getLevel(ignored: RuleViolation): string {
		// All violations are errors
		return ERROR;
	}
}

const getSarifFormatter = (engine: string): SarifFormatter => {
	if (engine === ENGINE.ESLINT_CUSTOM) {
		// Expose the eslint-custom engine as eslint
		return new ESLintSarifFormatter(ENGINE.ESLINT);
	} else if (engine.startsWith(ENGINE.ESLINT)) {
		// All other eslint engines are exposed as-is
		return new ESLintSarifFormatter(engine);
	} else if (engine.startsWith(ENGINE.PMD)) {
		// Use the same formatter for pmd and pmd-custom
		return new PMDSarifFormatter(ENGINE.PMD);
	} else if (engine === ENGINE.RETIRE_JS) {
		return new RetireJsSarifFormatter(engine);
	} else {
		throw new Error(`Developer error. Unknown engine '${engine}'`);
	}
}

const constructSarif = async (results: RuleResult[]): Promise<string> => {
	// Obtain the catalog and pass it in, this avoids multiple initializations
	// when waiting for promises in parallel
	const catalog: RuleCatalog = await Controller.getCatalog();
	const sarif = {
		version: '2.1.0',
		$schema: 'http://json.schemastore.org/sarif-2.1.0',
		runs: [
		]
	};

	// Map of engine->RuleResult[]
	const filteredResults: Map<string, RuleResult[]> = new Map<string, RuleResult[]>();
	for (const r of results) {
		if (!filteredResults.has(r.engine)) {
			filteredResults.set(r.engine, []);
		}
		filteredResults.get(r.engine).push(r);
	}

	for (const [engine, ruleResults] of filteredResults.entries()) {
		const formatter: SarifFormatter = getSarifFormatter(engine);
		sarif.runs.push(formatter.format(catalog, ruleResults));
	}

	return JSON.stringify(sarif);
}


export { constructSarif };

import * as fs from 'node:fs/promises';
import path from 'node:path';
import * as EngineApi from '@salesforce/code-analyzer-engine-api';

export class SampleEnginePlugin extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ['SampleEngine1', 'SampleEngine2', 'SampleEngine3'];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName === 'SampleEngine1') {
			this.createdEngines.set(engineName, new SampleEngine1(config));
		} else if (engineName === 'SampleEngine2') {
			this.createdEngines.set(engineName, new SampleEngine2(config));
		} else if (engineName === 'SampleEngine3') {
			this.createdEngines.set(engineName, new SampleEngine3(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`);
		}
		return Promise.resolve(this.getCreatedEngine(engineName));
	}

	getCreatedEngine(engineName: string): EngineApi.Engine {
		if (this.createdEngines.has(engineName)) {
			return this.createdEngines.get(engineName) as EngineApi.Engine;
		}
		throw new Error(`Engine with name ${engineName} has not yet been created`);
	}
}

export class SampleEngine1 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return 'SampleEngine1';
	}

	describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([
			{
				name: "stub1RuleA",
				severityLevel: EngineApi.SeverityLevel.Low,
				type: EngineApi.RuleType.Standard,
				tags: ["Recommended", "CodeStyle"],
				description: "Some description for stub1RuleA",
				resourceUrls: ["https://example.com/stub1RuleA"]
			},
			{
				name: "stub1RuleB",
				severityLevel: EngineApi.SeverityLevel.High,
				type: EngineApi.RuleType.Standard,
				tags: ["Recommended", "Security"],
				description: "Some description for stub1RuleB",
				resourceUrls: ["https://example.com/stub1RuleB"]
			},
			{
				name: "stub1RuleC",
				severityLevel: EngineApi.SeverityLevel.Moderate,
				type: EngineApi.RuleType.Standard,
				tags: ["Recommended", "Performance", "Custom"],
				description: "Some description for stub1RuleC",
				resourceUrls: ["https://example.com/stub1RuleC"]
			},
			{
				name: "stub1RuleD",
				severityLevel: EngineApi.SeverityLevel.Low,
				type: EngineApi.RuleType.Standard,
				tags: ["CodeStyle"],
				description: "Some description for stub1RuleD",
				resourceUrls: ["https://example.com/stub1RuleD"]
			},
			{
				name: "stub1RuleE",
				severityLevel: EngineApi.SeverityLevel.Moderate,
				type: EngineApi.RuleType.Standard,
				tags: ["Performance"],
				description: "Some description for stub1RuleE",
				resourceUrls: ["https://example.com/stub1RuleE", "https://example.com/stub1RuleE_2"]
			}
		]);
	}

	async runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		await new Promise(res => setTimeout(res, 250));
		this.runRulesCallHistory.push({ruleNames, runOptions});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 0
		});
		await new Promise(res => setTimeout(res, 250));
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "someMiscFineMessageFromStubEngine1",
			logLevel: EngineApi.LogLevel.Fine
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 50
		});
		await new Promise(res => setTimeout(res, 250));
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 100
		});
		await new Promise(res => setTimeout(res, 250));
		// Create violations for every rule-target pairing.
		const violations: EngineApi.Violation[] = [];
		for (const ruleName of ruleNames) {
			for (const target of await runOptions.workspace.getExpandedFiles()) {
				const firstEligibleFile = await resolveToTargetableFile(target);
				if (!firstEligibleFile) {
					throw new Error(`no files in ${target}`);
				}
				violations.push({
					ruleName,
					message: 'Fake Rule Fakily Violated',
					codeLocations: [{
						file: firstEligibleFile,
						startLine: 1,
						startColumn: 1
					}],
					primaryLocationIndex: 0
				});
			}
		}
		return {
			violations
		};
	}
}

/**
 * StubEngine2 - A sample engine stub with preconfigured outputs to help with testing
 */
export class SampleEngine2 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return "SampleEngine2";
	}

	describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([
			{
				name: "stub2RuleA",
				severityLevel: EngineApi.SeverityLevel.Moderate,
				type: EngineApi.RuleType.DataFlow,
				tags: ["Recommended", "Security"],
				description: "Some description for stub2RuleA",
				resourceUrls: ["https://example.com/stub2RuleA"]
			},
			{
				name: "stub2RuleB",
				severityLevel: EngineApi.SeverityLevel.Low,
				type: EngineApi.RuleType.DataFlow,
				tags: ["Performance", "Custom"],
				description: "Some description for stub2RuleB",
				resourceUrls: ["https://example.com/stub2RuleB"]
			},
			{
				name: "stub2RuleC",
				severityLevel: EngineApi.SeverityLevel.High,
				type: EngineApi.RuleType.DataFlow,
				tags: ["Recommended", "BestPractice"],
				description: "Some description for stub2RuleC",
				resourceUrls: [] // Purposely putting in nothing here
			}
		]);
	}

	async runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		this.runRulesCallHistory.push({ruleNames, runOptions});
		await new Promise(res => setTimeout(res, 1250));
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "someMiscInfoMessageFromStubEngine2",
			logLevel: EngineApi.LogLevel.Info
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 5
		});
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "Oh no! Something might be wrong (not actually, though)!",
			logLevel: EngineApi.LogLevel.Warn
		});
		await new Promise(res => setTimeout(res, 550));
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 63
		});

		await new Promise(res => setTimeout(res, 250));
		// Create violations for every rule-target pairing.
		const violations: EngineApi.Violation[] = [];
		for (const ruleName of ruleNames) {
			for (const target of await runOptions.workspace.getExpandedFiles()) {
				const firstEligibleFile = await resolveToTargetableFile(target);
				if (!firstEligibleFile) {
					throw new Error(`no files in ${target}`);
				}
				violations.push({
					ruleName,
					message: 'Fake Rule Fakily Violated',
					codeLocations: [{
						file: firstEligibleFile,
						startLine: 1,
						startColumn: 1
					}],
					primaryLocationIndex: 0
				});
			}
		}
		return {
			violations
		};
	}
}

export class SampleEngine3 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return 'SampleEngine3';
	}

	describeRules(): Promise<EngineApi.RuleDescription[]> {
		return Promise.resolve([
			{
				name: 'stub3RuleA',
				severityLevel: EngineApi.SeverityLevel.High,
				type: EngineApi.RuleType.DataFlow,
				tags: ['Recommended', 'CodeStyle'],
				description: 'Some description',
				resourceUrls: ['https://example.com/stub3rulea']
			}, {
				name: 'stub3RuleB',
				severityLevel: EngineApi.SeverityLevel.Low,
				type: EngineApi.RuleType.DataFlow,
				tags: ['Recommended', 'CodeStyle'],
				description: 'Some description',
				resourceUrls: ['https://example.com/stub3rulea']
			}
		]);
	}

	runRules(_ruleNames: string[], _runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		return Promise.resolve({
			violations: []
		});
	}
}

async function resolveToTargetableFile(fileOrDir: string): Promise<string> {
	if ((await fs.stat(fileOrDir)).isFile()) {
		return fileOrDir;
	}

	const dirContents = await fs.readdir(fileOrDir);
	for (const file of dirContents) {
		if ((await fs.stat(path.join(fileOrDir, file))).isFile()) {
			return path.join(fileOrDir, file);
		}
		const recursiveResult = await resolveToTargetableFile(path.join(fileOrDir, file));
		if (recursiveResult) {
			return recursiveResult;
		}
	}
	return '';
}

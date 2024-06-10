import * as EngineApi from '@salesforce/code-analyzer-engine-api';

export class SampleEnginePlugin extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ['SampleEngine1', 'SampleEngine2'];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): EngineApi.Engine {
		if (engineName === 'SampleEngine1') {
			this.createdEngines.set(engineName, new SampleEngine1(config));
		} else if (engineName === 'SampleEngine2') {
			this.createdEngines.set(engineName, new SampleEngine2(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`);
		}
		return this.getCreatedEngine(engineName);
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
	resultsToReturn: EngineApi.EngineRunResults = {
		violations: []
	};

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return 'SampleEngine1';
	}

	describeRules(): EngineApi.RuleDescription[] {
		return [
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
		];
	}

	runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): EngineApi.EngineRunResults {
		this.runRulesCallHistory.push({ruleNames, runOptions});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 0
		});
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "someMiscFineMessageFromStubEngine1",
			logLevel: EngineApi.LogLevel.Fine
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 50
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 100
		});
		return this.resultsToReturn;
	}
}

/**
 * StubEngine2 - A sample engine stub with preconfigured outputs to help with testing
 */
export class SampleEngine2 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];
	resultsToReturn: EngineApi.EngineRunResults = { violations: [] }

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return "SampleEngine2";
	}

	describeRules(): EngineApi.RuleDescription[] {
		return [
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
		];
	}

	runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): EngineApi.EngineRunResults {
		this.runRulesCallHistory.push({ruleNames, runOptions});
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "someMiscInfoMessageFromStubEngine2",
			logLevel: EngineApi.LogLevel.Info
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 5
		});
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 63
		});
		return this.resultsToReturn;
	}
}

import * as EngineApi from "@salesforce/code-analyzer-engine-api"
import {LogLevel} from "@salesforce/code-analyzer-engine-api"

/**
 * StubEnginePlugin - A plugin stub with preconfigured outputs to help with testing
 */
export class FunctionalStubEnginePlugin1 extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ["stubEngine1", "stubEngine2"];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): EngineApi.Engine {
		if (engineName == "stubEngine1") {
			this.createdEngines.set(engineName, new StubEngine1(config));
		} else if (engineName == "stubEngine2") {
			this.createdEngines.set(engineName, new StubEngine2(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`)
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

/**
 * StubEngine1 - A sample engine stub with preconfigured outputs to help with testing
 */
export class StubEngine1 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];
	resultsToReturn: EngineApi.EngineRunResults = { violations: [] }

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return "stubEngine1";
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
			logLevel: LogLevel.Fine
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
export class StubEngine2 extends EngineApi.Engine {
	readonly config: EngineApi.ConfigObject;
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];
	resultsToReturn: EngineApi.EngineRunResults = { violations: [] }

	constructor(config: EngineApi.ConfigObject) {
		super();
		this.config = config;
	}

	getName(): string {
		return "stubEngine2";
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
			logLevel: LogLevel.Info
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

/**
 * ThrowingPlugin1 - A plugin that throws an exception during a call to getAvailableEngineNames
 */
export class ThrowingStubPlugin1 extends EngineApi.EnginePluginV1 {
	getAvailableEngineNames(): string[] {
		throw new Error('SomeErrorFromGetAvailableEngineNames');
	}

	createEngine(_engineName: string, _config: EngineApi.ConfigObject): EngineApi.Engine {
		throw new Error('Should not be called');
	}
}

import * as EngineApi from "@salesforce/code-analyzer-engine-api"
import {LogLevel} from "@salesforce/code-analyzer-engine-api"

/**
 * FunctionalStubEnginePlugin1 - A plugin stub with preconfigured outputs to help with testing
 */
export class FunctionalStubEnginePlugin1 extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ["stubEngine1", "stubEngine2"];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName == "stubEngine1") {
			this.createdEngines.set(engineName, new StubEngine1(config));
		} else if (engineName == "stubEngine2") {
			this.createdEngines.set(engineName, new StubEngine2(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`)
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

/**
 * ConfigurableStubEnginePlugin1 - A plugin that can accept pre-created engines in place of calling {@link createEngine} on hard-coded engines.
 */
export class ConfigurableStubEnginePlugin1 extends EngineApi.EnginePluginV1 {
	private createdEngines: Map<string, EngineApi.Engine> = new Map();

	addEngine(engine: EngineApi.Engine): void {
		this.createdEngines.set(engine.getName(), engine);
	}

	getAvailableEngineNames(): string[] {
		// Return the names of whatever engines we've been configured with.
		return [...this.createdEngines.keys()];
	}

	createEngine(engineName: string, _config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		// The engines have already been created, so just check whether we have it.
		if (!this.createdEngines.has(engineName)) {
			throw new Error(`Plugin not preconfigured with engine ${engineName}`);
		}
		return Promise.resolve(this.createdEngines.get(engineName) as EngineApi.Engine);
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

	runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
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
		return Promise.resolve(this.resultsToReturn);
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

	runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
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
		return Promise.resolve(this.resultsToReturn);
	}
}

/**
 * ThrowingStubPlugin1 - A plugin that throws an exception during a call to getAvailableEngineNames
 */
export class ThrowingStubPlugin1 extends EngineApi.EnginePluginV1 {
	getAvailableEngineNames(): string[] {
		throw new Error('SomeErrorFromGetAvailableEngineNames');
	}

	createEngine(_engineName: string, _config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		throw new Error('Should not be called');
	}
}

/**
 * TimeableStubEnginePlugin1 - A plugin whose associated engine can be given a time to wait between engine update events.
 */
export class TimeableStubEnginePlugin1 extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ["timeableEngine1"];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName == "timeableEngine1") {
			this.createdEngines.set(engineName, new TimeableEngine1(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`)
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

/**
 * TimeableEngine1 - An engine with a {@code setWaitTime()} method allowing a delay to be added between update events.
 */
export class TimeableEngine1 extends EngineApi.Engine {
	private waitTime: number;

	constructor(config: EngineApi.ConfigObject) {
		super();
	}

	getName(): string {
		return "timeableEngine1";
	}

	setWaitTime(waitTime: number) {
		this.waitTime = waitTime;
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
			}
		]);
	}

	async runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		await new Promise(res => setTimeout(res, this.waitTime));
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 0
		});
		await new Promise(res => setTimeout(res, this.waitTime));
		this.emitEvent<EngineApi.LogEvent>({
			type: EngineApi.EventType.LogEvent,
			message: "someMiscFineMessageFromStubEngine1",
			logLevel: LogLevel.Fine
		});
		await new Promise(res => setTimeout(res, this.waitTime));
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 50
		});
		await new Promise(res => setTimeout(res, this.waitTime));
		this.emitEvent<EngineApi.ProgressEvent>({
			type: EngineApi.EventType.ProgressEvent,
			percentComplete: 100
		});
		await new Promise(res => setTimeout(res, this.waitTime));
		return Promise.resolve({violations: []});
	}
}

/**
 * EventConfigurableEngine1 - An engine with a {@code setWaitTime()} method allowing a delay to be added between update events.
 */
export class EventConfigurableEngine1 extends EngineApi.Engine {
	private events: {logLevel: LogLevel, message: string}[] = [];

	constructor(config: EngineApi.ConfigObject) {
		super();
	}

	getName(): string {
		return "eventConfigurableEngine1";
	}

	addEvents(...events: {logLevel: LogLevel, message: string}[]): void {
		this.events = [...this.events, ...events];
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
			}
		]);
	}

	async runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		for (const {logLevel, message} of this.events) {
			this.emitEvent<EngineApi.LogEvent>({
				type: EngineApi.EventType.LogEvent,
				message,
				logLevel
			});
		}
		return Promise.resolve({violations: []});
	}
}

export class StubEnginePluginWithTargetDependentEngine extends EngineApi.EnginePluginV1 {
	private readonly createdEngines: Map<string, EngineApi.Engine> = new Map();

	getAvailableEngineNames(): string[] {
		return ['targetDependentEngine1'];
	}

	createEngine(engineName: string, config: EngineApi.ConfigObject): Promise<EngineApi.Engine> {
		if (engineName === 'targetDependentEngine1') {
			this.createdEngines.set(engineName, new TargetDependentEngine1(config));
		} else {
			throw new Error(`Unsupported engine name: ${engineName}`);
		}
		return Promise.resolve(this.getCreatedEngine(engineName));
	}

	getCreatedEngine(engineName: string): EngineApi.Engine {
		if (this.createdEngines.has(engineName)) {
			return this.createdEngines.get(engineName) as EngineApi.Engine;
		}
		throw new Error(`Engine with name ${engineName} has not yet been created.`);
	}
}

export class TargetDependentEngine1 extends EngineApi.Engine {
	readonly runRulesCallHistory: {ruleNames: string[], runOptions: EngineApi.RunOptions}[] = [];
	constructor(_config: EngineApi.ConfigObject) {
		super();
	}

	getName(): string {
		return 'targetDependentEngine1';
	}

	describeRules(describeOptions: EngineApi.DescribeOptions): Promise<EngineApi.RuleDescription[]> {
		if (!describeOptions.workspace) {
			return Promise.resolve([]);
		}
		// Derive a rule for each of the targeted files/folders in the workspace.
		return Promise.resolve(describeOptions.workspace.getFilesAndFolders().map(fileOrFolder => {
			return {
				name: `ruleFor${fileOrFolder}`,
				severityLevel: EngineApi.SeverityLevel.Low,
				type: EngineApi.RuleType.Standard,
				tags: ["Recommended"],
				description: `Rule synthesized for target "${fileOrFolder}`,
				resourceUrls: [`https://example.com/${fileOrFolder}`]
			}
		}));
	}

	runRules(ruleNames: string[], runOptions: EngineApi.RunOptions): Promise<EngineApi.EngineRunResults> {
		this.runRulesCallHistory.push({ruleNames, runOptions});
		return Promise.resolve({violations: []});
	}
}

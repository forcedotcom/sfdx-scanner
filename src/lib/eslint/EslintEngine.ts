import { BaseEslintEngine } from "./BaseEslintEngine";
import { JavascriptEslintStrategy } from "./JavascriptEslintStrategy";
import { LWCEslintStrategy } from "./LWCEslintStrategy";
import { TypescriptEslintStrategy } from "./TypescriptEslintStrategy";


export class JavascriptEslintEngine extends BaseEslintEngine {

	async init(): Promise<void> {
		const strategy = new JavascriptEslintStrategy();
		await strategy.init();
		await super.initializeContents(strategy);
	}

}

export class LWCEslintEngine extends BaseEslintEngine {

	async init(): Promise<void> {
		const strategy = new LWCEslintStrategy();
		await strategy.init();
		await super.initializeContents(strategy);
	}

}

export class TypescriptEslintEngine extends BaseEslintEngine {

	async init(): Promise<void> {
		const strategy = new TypescriptEslintStrategy();
		await strategy.init();
		await super.initializeContents(strategy);
	}

}

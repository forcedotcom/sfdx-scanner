import { BaseEslintEngine } from "./BaseEslintEngine";
import { JavascriptEslintStrategy } from "./JavascriptEslintStrategy";
import { TypescriptEslintStrategy } from "./TypescriptEslintStrategy";


export class JavascriptEslintEngine extends BaseEslintEngine {

	public async init(): Promise<void> {
		const strategy = new JavascriptEslintStrategy();
		await strategy.init();
		await super.initializeContents(strategy);
	}

}

export class TypescriptEslintEngine extends BaseEslintEngine {

	public async init(): Promise<void> {
		const strategy = new TypescriptEslintStrategy();
		await strategy.init();
		await super.initializeContents(strategy);
	}

}

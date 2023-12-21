import {Messages} from "@salesforce/core";
import {getSingleton} from "./ioc.config";
import {Services} from "./Constants";
import {Tokens} from "@salesforce/core/lib/messages";


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

export enum Bundle {
	Add = "add",
	Config = "Config",
	Controller = 'Controller',
	CpdEngine = "CpdEngine",
	CustomEslintEngine = "CustomEslintEngine",
	CustomRulePathManager = "CustomRulePathManager",
	Common = "common",
	CommonRun = "run-common",
	DefaultRuleManager = "DefaultRuleManager",
	Describe = "describe",
	EslintEngine = "eslintEngine",
	EventKeyTemplates = "EventKeyTemplates",
	Exceptions = "Exceptions",
	JreSetupManager = "jreSetupManager",
	List = "list",
	PmdEngine = "PmdEngine",
	PmdLanguageManager = "PmdLanguageManager",
	Remove = "remove",
	Run = "run-pathless",
	RunDfa = "run-dfa",
	RunOutputProcessor = "RunOutputProcessor",
	SfgeEngine = "SfgeEngine",
	TypescriptEslintStrategy = "TypescriptEslintStrategy",
	VersionUpgradeManager = "VersionUpgradeManager"
}

export class MessageCatalog {
	private readonly bundleMap: Map<Bundle, Messages<string>> = new Map();

	public getMessage(bundle: Bundle, messageKey: string, tokens?: Tokens): string {
		return this.getBundle(bundle).getMessage(messageKey, tokens);
	}

	private getBundle(bundle: Bundle): Messages<string> {
		if (!this.bundleMap.has(bundle)) {
			this.bundleMap.set(bundle, Messages.loadMessages('@salesforce/sfdx-scanner', bundle.toString()));
		}
		return this.bundleMap.get(bundle);
	}
}

/**
 * This is a convenience method to get a message from a message bundle from the registered MessageCatalog
 */
export function getMessage(bundle: Bundle, messageKey: string, tokens?: Tokens): string {
	return getSingleton<MessageCatalog>(Services.MessageCatalog).getMessage(bundle, messageKey, tokens);
}

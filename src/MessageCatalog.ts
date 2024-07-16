import {Messages} from "@salesforce/core";
import {Tokens} from "@salesforce/core/lib/messages";


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

export enum BundleName {
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
	RetireJsEngine = 'RetireJsEngine',
	Run = "run-pathless",
	RunDfa = "run-dfa",
	RunOutputProcessor = "RunOutputProcessor",
	SfgeEngine = "SfgeEngine",
	TypescriptEslintStrategy = "TypescriptEslintStrategy",
	VersionUpgradeManager = "VersionUpgradeManager"
}

class MessageCatalog {
	private readonly bundleMap: Map<BundleName, Messages<string>> = new Map();

	public getMessage(bundle: BundleName, messageKey: string, tokens?: Tokens): string {
		return this.getBundle(bundle).getMessage(messageKey, tokens);
	}

	private getBundle(bundle: BundleName): Messages<string> {
		if (!this.bundleMap.has(bundle)) {
			this.bundleMap.set(bundle, Messages.loadMessages('@salesforce/sfdx-scanner', bundle.toString()));
		}
		return this.bundleMap.get(bundle);
	}
}

let INSTANCE: MessageCatalog = null;

/**
 * This is a convenience method to get a message from a message bundle from the registered MessageCatalog
 */
export function getMessage(bundle: BundleName, messageKey: string, tokens?: Tokens): string {
	INSTANCE = INSTANCE || new MessageCatalog();
	return INSTANCE.getMessage(bundle, messageKey, tokens);
}

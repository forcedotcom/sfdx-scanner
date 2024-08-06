import {Messages} from '@salesforce/core';
import {Tokens} from '@salesforce/core/lib/messages';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

export enum BundleName {
	InitCommand = 'init-command',
	OutputFileWriter = 'output-file-writer',
	PathStartUtil = 'path-start-util',
	ProgressEventListener = 'progress-event-listener',
	ResultsViewer = 'results-viewer',
	RuleViewer = 'rule-viewer',
	RulesCommand = 'rules-command',
	RunAction = 'run-action',
	RunCommand = 'run-command'
}

class MessageCatalog {
	private readonly bundleMap: Map<BundleName, Messages<string>> = new Map();

	public getMessage(bundle: BundleName, messageKey: string, tokens?: Tokens): string {
		return this.getBundle(bundle).getMessage(messageKey, tokens);
	}

	public getMessages(bundle: BundleName, messageKey: string, tokens?: Tokens): string[] {
		return this.getBundle(bundle).getMessages(messageKey, tokens);
	}

	private getBundle(bundle: BundleName): Messages<string> {
		if (!this.bundleMap.has(bundle)) {
			this.bundleMap.set(bundle, Messages.loadMessages('@salesforce/plugin-code-analyzer', bundle.toString()));
		}
		// @ts-expect-error Map.get() can technically return undefined, but that will never happen in practice.
		return this.bundleMap.get(bundle);
	}
}

let INSTANCE: MessageCatalog;

export function getMessage(bundle: BundleName, messageKey: string, tokens?: Tokens): string {
	INSTANCE = INSTANCE || new MessageCatalog();
	return INSTANCE.getMessage(bundle, messageKey, tokens);
}

export function getMessages(bundle: BundleName, messageKey: string, tokens?: Tokens): string[] {
	INSTANCE = INSTANCE || new MessageCatalog();
	return INSTANCE.getMessages(bundle, messageKey, tokens);
}

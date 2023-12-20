import {Messages} from "@salesforce/core";
import {getSingleton} from "./ioc.config";
import {Services} from "./Constants";


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// TODO: Add all the bundles here
export enum BUNDLE {
	COMMON = "common",
	COMMON_RUN = "run-common",
	RUN = "run-pathless",
	RUN_DFA = "run-dfa"
}

export class MessageCatalog {
	private readonly bundleMap: Map<BUNDLE, Messages<string>> = new Map();

	public getMessage(bundle: BUNDLE, messageKey: string): string {
		return this.getBundle(bundle).getMessage(messageKey);
	}

	private getBundle(bundle: BUNDLE): Messages<string> {
		if (!this.bundleMap.has(bundle)) {
			this.bundleMap.set(bundle, Messages.loadMessages('@salesforce/sfdx-scanner', bundle.toString()));
		}
		return this.bundleMap.get(bundle);
	}
}

export function getBundledMessage(bundle: BUNDLE, messageKey: string): string {
	return getSingleton<MessageCatalog>(Services.MessageCatalog).getMessage(bundle, messageKey);
}

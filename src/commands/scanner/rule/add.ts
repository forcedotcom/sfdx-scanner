import {Flags} from '@salesforce/sf-plugins-core';
import {Messages, SfError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {stringArrayTypeGuard} from '../../../lib/util/Utils';
import {Controller} from '../../../Controller';
import path = require('path');
import untildify = require('untildify');
import { ScannerCommand } from '../../../lib/ScannerCommand';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'add');

export default class Add extends ScannerCommand {

	public static summary = messages.getMessage('commandSummary');
	public static description = messages.getMessage('commandDescription');

	public static examples = [
		messages.getMessage('examples')
	];

	public static readonly flags = {
		language: Flags.string({
			char: 'l',
			summary: messages.getMessage('flags.languageSummary'),
			description: messages.getMessage('flags.languageDescription'),
			required: true
		}),
		path: Flags.custom<string[]>({
			char: 'p',
			summary: messages.getMessage('flags.pathSummary'),
			description: messages.getMessage('flags.pathDescription'),
			multiple: true,
			delimiter: ',',
			required: true
		})()
	};

	async runInternal(): Promise<AnyJson> {
		this.validateFlags();

		const language = this.parsedFlags.language as string;
		const paths = this.resolvePaths();

		this.logger.trace(`Language: ${language}`);
		this.logger.trace(`Rule path: ${JSON.stringify(paths)}`);

		// Add to Custom Classpath registry
		const manager = await Controller.createRulePathManager();
		const classpathEntries = await manager.addPathsForLanguage(language, paths);
		this.display.displayInfo(`Successfully added rules for ${language}.`);
		this.display.displayInfo(`${classpathEntries.length} Path(s) added: ${classpathEntries.toString()}`);
		return {success: true, language, path: classpathEntries};
	}

	private validateFlags(): void {
		if ((this.parsedFlags.language as string).length === 0) {
			throw new SfError(messages.getMessage('validations.languageCannotBeEmpty', []));
		}
		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (this.parsedFlags.path && stringArrayTypeGuard(this.parsedFlags.path) && (!this.parsedFlags.path.length || this.parsedFlags.path.includes(''))) {
			throw new SfError(messages.getMessage('validations.pathCannotBeEmpty', []));
		}
	}

	private resolvePaths(): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return (this.parsedFlags.path as string[]).map(p => path.resolve(untildify(p)));
	}
}

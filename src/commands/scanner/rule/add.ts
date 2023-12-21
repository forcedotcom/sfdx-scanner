import {Flags} from '@salesforce/sf-plugins-core';
import {SfError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {stringArrayTypeGuard} from '../../../lib/util/Utils';
import {Controller} from '../../../Controller';
import path = require('path');
import untildify = require('untildify');
import { ScannerCommand } from '../../../lib/ScannerCommand';
import {Bundle, getMessage} from "../../../MessageCatalog";

export default class Add extends ScannerCommand {

	public static summary = getMessage(Bundle.Add, 'commandSummary');
	public static description = getMessage(Bundle.Add, 'commandDescription');

	public static examples = [
		getMessage(Bundle.Add, 'examples')
	];

	public static readonly flags = {
		language: Flags.string({
			char: 'l',
			summary: getMessage(Bundle.Add, 'flags.languageSummary'),
			description: getMessage(Bundle.Add, 'flags.languageDescription'),
			required: true
		}),
		path: Flags.custom<string[]>({
			char: 'p',
			summary: getMessage(Bundle.Add, 'flags.pathSummary'),
			description: getMessage(Bundle.Add, 'flags.pathDescription'),
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
			throw new SfError(getMessage(Bundle.Add, 'validations.languageCannotBeEmpty', []));
		}
		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (this.parsedFlags.path && stringArrayTypeGuard(this.parsedFlags.path) && (!this.parsedFlags.path.length || this.parsedFlags.path.includes(''))) {
			throw new SfError(getMessage(Bundle.Add, 'validations.pathCannotBeEmpty', []));
		}
	}

	private resolvePaths(): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return (this.parsedFlags.path as string[]).map(p => path.resolve(untildify(p)));
	}
}

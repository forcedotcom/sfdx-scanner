import {Flags} from '@salesforce/sf-plugins-core';
import {AnyJson} from '@salesforce/ts-types';
import {Controller} from '../../../Controller';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {Bundle, getMessage} from "../../../MessageCatalog";
import {Inputs} from "../../../types";
import {Config} from "@oclif/core";
import {PathResolver, PathResolverImpl} from "../../../lib/PathResolver";

import {InputValidatorFactory, RuleAddCommandInputValidatorFactory} from "../../../lib/InputValidator";

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

	public readonly pathResolver: PathResolver;

	public constructor(argv: string[], config: Config,
					   inputValidatorFactory?: InputValidatorFactory,
					   pathResolver?: PathResolver) {
		if (typeof inputValidatorFactory === 'undefined') {
			inputValidatorFactory = new RuleAddCommandInputValidatorFactory();
		}
		if (typeof pathResolver === 'undefined') {
			pathResolver = new PathResolverImpl();
		}
		super(argv, config, inputValidatorFactory);
		this.pathResolver = pathResolver;
	}

	async runInternal(inputs: Inputs): Promise<AnyJson> {
		const language = inputs.language as string;
		const paths = this.pathResolver.resolvePaths(inputs);

		this.logger.trace(`Language: ${language}`);
		this.logger.trace(`Rule path: ${JSON.stringify(paths)}`);

		// Add to Custom Classpath registry
		const manager = await Controller.createRulePathManager();
		const classpathEntries = await manager.addPathsForLanguage(language, paths);

		this.display.displayInfo(`Successfully added rules for ${language}.`);
		this.display.displayInfo(`${classpathEntries.length} Path(s) added: ${classpathEntries.toString()}`);
		return {success: true, language, path: classpathEntries};
	}
}

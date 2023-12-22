import {Action} from "../ScannerCommand";
import {Display} from "../Display";
import {Inputs} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {Logger, SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";
import {stringArrayTypeGuard} from "../util/Utils";
import {PathResolver} from "../PathResolver";
import {Controller} from "../../Controller";

export class RuleAddAction implements Action {
	private readonly logger: Logger;
	private readonly display: Display;
	private readonly pathResolver: PathResolver;

	public constructor(logger: Logger, display: Display, pathResolver: PathResolver) {
		this.logger = logger;
		this.display = display;
		this.pathResolver = pathResolver;
	}

	public async validateInputs(inputs: Inputs): Promise<void> {
		if ((inputs.language as string).length === 0) {
			throw new SfError(getMessage(Bundle.Add, 'validations.languageCannotBeEmpty', []));
		}

		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (inputs.path && stringArrayTypeGuard(inputs.path) && (!inputs.path.length || inputs.path.includes(''))) {
			throw new SfError(getMessage(Bundle.Add, 'validations.pathCannotBeEmpty', []));
		}
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
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

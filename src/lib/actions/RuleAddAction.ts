import {Action} from "../ScannerCommand";
import {Display} from "../Display";
import {Inputs} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {stringArrayTypeGuard} from "../util/Utils";
import {InputsResolver} from "../InputsResolver";
import {Controller} from "../../Controller";

/**
 * The Action behind the "rule add" command
 */
export class RuleAddAction implements Action {
	private readonly logger: Logger;
	private readonly display: Display;
	private readonly inputsResolver: InputsResolver;

	public constructor(logger: Logger, display: Display, inputsResolver: InputsResolver) {
		this.logger = logger;
		this.display = display;
		this.inputsResolver = inputsResolver;
	}

	public validateInputs(inputs: Inputs): Promise<void> {
		if ((inputs.language as string).length === 0) {
			throw new SfError(getMessage(BundleName.Add, 'validations.languageCannotBeEmpty', []));
		}

		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (inputs.path && stringArrayTypeGuard(inputs.path) && (!inputs.path.length || inputs.path.includes(''))) {
			throw new SfError(getMessage(BundleName.Add, 'validations.pathCannotBeEmpty', []));
		}

		return Promise.resolve();
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
		const language = inputs.language as string;
		const paths = this.inputsResolver.resolvePaths(inputs);

		this.logger.trace(`Language: ${language}`);
		this.logger.trace(`Rule path: ${JSON.stringify(paths)}`);

		// TODO: Inject RulePathManager as a dependency to improve testability by removing coupling to runtime implementation
		const manager = await Controller.createRulePathManager();
		const classpathEntries = await manager.addPathsForLanguage(language, paths);

		this.display.displayInfo(`Successfully added rules for ${language}.`);
		this.display.displayInfo(`${classpathEntries.length} Path(s) added: ${classpathEntries.toString()}`);
		return {success: true, language, path: classpathEntries};
	}
}

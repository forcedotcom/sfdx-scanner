import {Action} from "../ScannerCommand";
import {Display} from "../Display";
import {Inputs} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {stringArrayTypeGuard} from "../util/Utils";
import {InputProcessor} from "../InputProcessor";
import {Controller} from "../../Controller";

/**
 * The Action behind the "rule add" command
 */
export class RuleAddAction implements Action {
	private readonly logger: Logger;
	private readonly display: Display;
	private readonly inputProcessor: InputProcessor;

	public constructor(logger: Logger, display: Display, inputProcessor: InputProcessor) {
		this.logger = logger;
		this.display = display;
		this.inputProcessor = inputProcessor;
	}

	public validateInputs(inputs: Inputs): Promise<void> {
		if (!inputs.language || (inputs.language as string).length === 0) {
			throw new SfError(getMessage(BundleName.Add, 'validations.languageCannotBeEmpty', []));
		}

		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (!inputs.path || inputs.path && stringArrayTypeGuard(inputs.path) && (!inputs.path.length || inputs.path.includes(''))) {
			throw new SfError(getMessage(BundleName.Add, 'validations.pathCannotBeEmpty', []));
		}

		return Promise.resolve();
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
		const language = inputs.language as string;
		const paths = this.inputProcessor.resolvePaths(inputs);

		this.logger.trace(`Language: ${language}`);
		this.logger.trace(`Rule path: ${JSON.stringify(paths)}`);

		// TODO: Inject RulePathManager as a dependency to improve testability by removing coupling to runtime implementation
		const manager = await Controller.createRulePathManager();
		const classpathEntries = await manager.addPathsForLanguage(language, paths);

		this.display.displayInfo(getMessage(BundleName.Add, 'output.successfullyAddedRules', [language]));
		this.display.displayInfo(getMessage(BundleName.Add, 'output.resultSummary', [classpathEntries.length, classpathEntries.toString()]));
		return {success: true, language, path: classpathEntries};
	}
}

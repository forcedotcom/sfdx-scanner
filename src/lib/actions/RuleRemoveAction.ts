import {Action} from "../ScannerCommand";
import {Inputs, Rule} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {stringArrayTypeGuard} from "../util/Utils";
import {Logger, SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";
import {Controller} from "../../Controller";
import {RuleFilter, SourcePackageFilter} from "../RuleFilter";
import {Display} from "../Display";
import {InputsResolver} from "../InputsResolver";

/**
 * The Action behind the "rule remove" command
 */
export class RuleRemoveAction implements Action {
	private readonly logger: Logger;
	private readonly display: Display;
	private readonly inputsResolver: InputsResolver;

	public constructor(logger: Logger, display: Display, inputsResolver: InputsResolver) {
		this.logger = logger;
		this.display = display;
		this.inputsResolver = inputsResolver;
	}

	public validateInputs(inputs: Inputs): Promise<void> {
		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (inputs.path && stringArrayTypeGuard(inputs.path) && (!inputs.path.length || inputs.path.includes(''))) {
			throw new SfError(getMessage(Bundle.Remove, 'validations.pathCannotBeEmpty'));
		}
		return Promise.resolve();
	}

	public async run(inputs: Inputs): Promise<AnyJson> {
		// Step 2: Pull out and process our flag.
		const paths = inputs.path ? this.inputsResolver.resolvePaths(inputs) : null;
		this.logger.trace(`Rule path: ${JSON.stringify(paths)}`);

		// Step 3: Get all rule entries matching the criteria they provided.
		// TODO: Inject RulePathManager as a dependency to improve testability by removing coupling to runtime implementation
		const crpm = await Controller.createRulePathManager();
		const deletablePaths: string[] = paths ? await crpm.getMatchingPaths(paths) : crpm.getAllPaths();

		// Step 4: If there aren't any matching paths, we need to react appropriately.
		if (!deletablePaths || deletablePaths.length === 0) {
			// If the --path flag was used, then we couldn't find any paths matching their criteria, and we should throw
			// and error saying as much.
			if (paths) {
				throw new SfError(getMessage(Bundle.Remove, 'errors.noMatchingPaths'));
			} else {
				// If the flag wasn't used, then they're just doing a dry run. We should still let them know that they
				// don't have anything, but it should be surfaced as a log instead of an error.
				this.display.displayInfo(getMessage(Bundle.Remove, 'output.dryRunReturnedNoRules'));
				return [];
			}
		}

		// Step 5: If the --path flag was NOT used, they want to do a dry run. We should let them know all of the custom
		// rules they've defined.
		if (!paths) {
			this.display.displayInfo(this.generateDryRunOutput(deletablePaths));
			return [];
		}

		// Step 6: Unless the --force flag was used, we'll want to identify all of the rules that are defined in the entries
		// they want to delete, and force them to confirm that they're really sure.
		if (!inputs.force) {
			// Step 6a: We'll want to create filter criteria.
			const filters: RuleFilter[] = [];
			filters.push(new SourcePackageFilter(deletablePaths));

			// Step 6b: We'll want to retrieve the matching rules.
			// TODO: Inject RuleManager as a dependency to improve testability by removing coupling to runtime implementation
			const rm = await Controller.createRuleManager();
			const matchingRules: Rule[] = await rm.getRulesMatchingCriteria(filters);

			// Step 6c: If any rules are found, ask the user to confirm that they actually want to delete them.
			if (matchingRules.length > 0 && await this.display.displayConfirmationPrompt(this.generateConfirmationPrompt(matchingRules)) === false) {
				this.display.displayInfo(getMessage(Bundle.Remove, 'output.aborted'));
				return [];
			}
		}

		// Step 7: Actually delete the entries.
		const deletedPaths = await crpm.removePaths(paths);

		// Step 8: Output. We'll display a message indicating which entries were deleted, and we'll return that array for
		// the --json flag.
		this.display.displayInfo(getMessage(Bundle.Remove, 'output.resultSummary', [deletedPaths.join(', ')]));
		return deletedPaths;
	}

	private generateConfirmationPrompt(rules: Rule[]): string {
		// We'll want to create a list of short strings containing the name of each rule and where it's defined, so we
		// can log that out to the user.
		const ruleDescriptions: string[] = rules.map(rule => getMessage(Bundle.Remove, 'output.ruleTemplate', [rule.name, rule.sourcepackage]));
		return getMessage(Bundle.Remove, 'output.deletionPrompt', [ruleDescriptions.join('\n')]);
	}

	private generateDryRunOutput(paths: string[]): string {
		const pathString = paths.map(p => getMessage(Bundle.Remove, 'output.dryRunRuleTemplate', [p])).join('\n');
		return getMessage(Bundle.Remove, 'output.dryRunOutput', [paths.length, pathString]);

	}
}

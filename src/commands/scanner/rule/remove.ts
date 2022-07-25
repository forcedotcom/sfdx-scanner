import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {Controller} from '../../../Controller';
import {END_OF_LIFE_BANNER, PILOT_AVAILABILITY_BANNER} from '../../../Constants';
import {RuleFilter, SourcePackageFilter} from '../../../lib/RuleFilter';
import {ScannerCommand} from '../../../lib/ScannerCommand';
import {Rule} from '../../../types';
import path = require('path');
import untildify = require('untildify');

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'remove');

export default class Remove extends ScannerCommand {
	// These determine what's displayed when the --help/-h flag is supplied.
	public static description = messages.getMessage('commandDescription');
	public static longDescription = messages.getMessage('commandDescriptionLong');

	public static examples = [
		messages.getMessage('examples')
	];

	public static args = [{name: 'file'}];

	// This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
	// is what's printed when the -h/--help flag is supplied.
	protected static flagsConfig = {
		verbose: flags.builtin(),
		force: flags.boolean({
			char: 'f',
			description: messages.getMessage('flags.forceDescription'),
			longDescription: messages.getMessage('flags.forceDescriptionLong')
		}),
		path: flags.array({
			char: 'p',
			description: messages.getMessage('flags.pathDescription'),
			longDescription: messages.getMessage('flags.pathDescriptionLong')
		})
	};

	public async run(): Promise<AnyJson> {
		this.ux.warn(END_OF_LIFE_BANNER);
		this.ux.warn(PILOT_AVAILABILITY_BANNER);
		// Step 1: Validate our input.
		this.validateFlags();

		// Step 2: Pull out and process our flag.
		const paths = this.flags.path ? this.resolvePaths() : null;
		this.logger.trace(`Rule path: ${paths}`);

		// Step 3: Get all rule entries matching the criteria they provided.
		const crpm = await Controller.createRulePathManager();
		const deletablePaths: string[] = paths ? await crpm.getMatchingPaths(paths) : crpm.getAllPaths();

		// Step 4: If there aren't any matching paths, we need to react appropriately.
		if (!deletablePaths || deletablePaths.length === 0) {
			// If the --path flag was used, then we couldn't find any paths matching their criteria, and we should throw
			// and error saying as much.
			if (paths) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'remove', 'errors.noMatchingPaths');
			} else {
				// If the flag wasn't used, then they're just doing a dry run. We should still let them know that they
				// don't have anything, but it should be surfaced as a log instead of an error.
				this.ux.log(messages.getMessage('output.dryRunReturnedNoRules'));
				return [];
			}
		}

		// Step 5: If the --path flag was NOT used, they want to do a dry run. We should let them know all of the custom
		// rules they've defined.
		if (!paths) {
			this.ux.log(this.generateDryRunOutput(deletablePaths));
			return [];
		}

		// Step 6: Unless the --force flag was used, we'll want to identify all of the rules that are defined in the entries
		// they want to delete, and force them to confirm that they're really sure.
		if (!this.flags.force) {
			// Step 6a: We'll want to create filter criteria.
			const filters: RuleFilter[] = [];
			filters.push(new SourcePackageFilter(deletablePaths));

			// Step 6b: We'll want to retrieve the matching rules.
			const rm = await Controller.createRuleManager();
			const matchingRules: Rule[] = await rm.getRulesMatchingCriteria(filters);

			// Step 6c: If any rules are found, ask the user to confirm that they actually want to delete them.
			if (matchingRules.length > 0 && await this.ux.confirm(this.generateConfirmationPrompt(matchingRules)) === false) {
				this.ux.log(messages.getMessage('output.aborted'));
				return [];
			}
		}

		// Step 7: Actually delete the entries.
		const deletedPaths = await crpm.removePaths(paths);

		// Step 8: Output. We'll display a message indicating which entries were deleted, and we'll return that array for
		// the --json flag.
		this.ux.log(messages.getMessage('output.resultSummary', [deletedPaths.join(', ')]));
		return deletedPaths;
	}

	private validateFlags(): void {
		// --path '' results in different values depending on the OS. On Windows it is [], on *nix it is [""]
		if (this.flags.path && (!this.flags.path.length || this.flags.path.includes(''))) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'remove', 'validations.pathCannotBeEmpty', []);
		}
	}

	private resolvePaths(): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return this.flags.path.map(p => path.resolve(untildify(p)));
	}

	private generateConfirmationPrompt(rules: Rule[]): string {
		// We'll want to create a list of short strings containing the name of each rule and where it's defined, so we
		// can log that out to the user.
		const ruleDescriptions: string[] = rules.map(rule => messages.getMessage('output.ruleTemplate', [rule.name, rule.sourcepackage]));
		return messages.getMessage('output.deletionPrompt', [rules.length, ruleDescriptions.join('\n')]);
	}

	private generateDryRunOutput(paths: string[]): string {
		const pathString = paths.map(p => messages.getMessage('output.dryRunRuleTemplate', [p])).join('\n');
		return messages.getMessage('output.dryRunOutput', [paths.length, pathString]);

	}
}

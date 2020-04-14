import {flags} from '@salesforce/command';
import {Messages, SfdxError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {ScannerCommand} from '../scannerCommand';
import path = require('path');
import {RuleFilter, RuleManager, RULE_FILTER_TYPE} from '../../../lib/RuleManager';
import untildify = require('untildify');
import {CustomRulePathManager} from '../../../lib/CustomRulePathManager';

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
			longDescription: messages.getMessage('flags.pathDescriptionLong'),
			required: true
		}),
		language: flags.string({
			char: 'l',
			description: messages.getMessage('flags.languageDescription'),
			longDescription: messages.getMessage('flags.languageDescriptionLong'),
			required: true
		})
	};

	public async run(): Promise<AnyJson> {
		// Step 1: Validate our input. Our language and path flags both need to be non-null.
		this.validateFlags();

		// Step 2: We need to resolve the paths we were given.
		const language = this.flags.language;
		const paths = this.resolvePaths();

		this.logger.trace(`Language: ${language}`);
		this.logger.trace(`Rule path: ${paths}`);

		// Step 3: The paths we were given aren't guaranteed to be the actual paths to rule definitions. They could be
		// going to folders, or entries that aren't mapped to a language, or to nothing at all. So we'll expand the list
		// into valid entry paths, then reduce that second list down to only entries that are currently mapped to the target
		// language.
		const crpm = await CustomRulePathManager.create({});
		const deletablePaths = await crpm.getMatchingPaths(language, paths);

		// Step 3a: If there aren't any deletable paths, we should log a message stating that and then return.
		if (!deletablePaths || deletablePaths.length === 0) {
			this.ux.log('PLACEHOLDER MESSAGE ABOUT NO DELETABLE PATHS');
		}

		// Step 4: Unless the --force flag was used, we'll want to identify all of the rules that are defined in the entries
		// they want to delete, and force them to confirm.
		if (!this.flags.force) {
			// Step 4a: We'll want to create filter criteria.
			const filters: RuleFilter[] = [];
			filters.push(new RuleFilter(RULE_FILTER_TYPE.LANGUAGE, [language]));
			filters.push(new RuleFilter(RULE_FILTER_TYPE.SOURCEPACKAGE, deletablePaths));

			// Step 4b: We'll want to retrieve the matching rules.
			const rm = await RuleManager.create({});
			const matchingRules = await rm.getRulesMatchingCriteria(filters);

			// Step 4c: Ask the user to confirm that they actually want to delete the rules in question.
			if (await this.ux.confirm('Placeholder. Do you want to delete ' + matchingRules.length + ' rules? (y/n)') === false) {
				return [];
			}
		}

		// Step 5: Actually delete the entries.
		const deletedPaths = await crpm.removePathsForLanguage(language, paths);

		// Step 6: Output. We'll display a message indicating which entries were deleted, and we'll return that array
		// for the --json flag.
		this.ux.log('Placeholder: Successfully deleted ' + deletedPaths.length + ' custom entries');
		return deletedPaths;
	}

	private validateFlags(): void {
		if (this.flags.language.length === 0) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'remove', 'validations.languageCannotBeEmpty', []);
		}
		if (this.flags.path.includes('')) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'remove', 'validations.pathCannotBeEmpty', []);
		}
	}

	private resolvePaths(): string[] {
		// path.resolve() turns relative paths into absolute paths. It accepts multiple strings, but this is a trap because
		// they'll be concatenated together. So we use .map() to call it on each path separately.
		return this.flags.path.map(p => path.resolve(untildify(p)));
	}
}

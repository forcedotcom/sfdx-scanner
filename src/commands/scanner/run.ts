import { flags, SfdxCommand } from '@salesforce/command';
import { Messages } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import {RULE_FILTER_TYPE, RuleFilter, RuleManager} from "../../lib/RuleManager";

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'run');

export default class Run extends SfdxCommand {
  // These determine what's displayed when the --help/-h flag is provided.
  public static description = messages.getMessage('commandDescription');
  // TODO: Write real examples.
  public static examples = [
    `$ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
  Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
  My hub org id is: 00Dxx000000001234
  `];

  public static args = [{name: 'file'}];

  // This defines the flags accepted by this command.
  protected static flagsConfig = {
    // These flags are how you choose which rules you're running.
    rulename: flags.string({
      char: 'n',
      description: messages.getMessage('flags.rulenameDescription'),
      // If you're specifying by name, it doesn't make sense to let you specify by any other means.
      exclusive: ['category', 'ruleset', 'severity', 'exclude-rule']
    }),
    category: flags.array({
      char: 'c',
      description: messages.getMessage('flags.categoryDescription')
    }),
    ruleset: flags.array({
      char: 'r',
      description: messages.getMessage('flags.rulesetDescription')
    }),
    severity: flags.string({
      char: 's',
      description: messages.getMessage('flags.severityDescription')
    }),
    'exclude-rule': flags.array({
      description: messages.getMessage('flags.excluderuleDescription')
    }),
    // These flags are how you choose which files you're targeting.
    file: flags.array({
      char: 'f',
      description: messages.getMessage('flags.fileDescription')
    }),
    directory: flags.array({
      char: 'd',
      description: messages.getMessage('flags.directoryDescription')
    }),
    exclude: flags.array({
      char: 'x',
      description: messages.getMessage('flags.excludeDescription')
    }),
    org: flags.string({
      char: 'a',
      description: messages.getMessage('flags.orgDescription'),
      // If you're specifying an org, it doesn't make sense to let you specify anything else.
      exclusive: ['file', 'directory', 'exclude']
    }),
    // These flags modify how the process runs, rather than what it consumes.
    'suppress-warnings': flags.boolean({
      description: messages.getMessage('flags.suppresswarningsDescription')
    })
  };

  public async run(): Promise<AnyJson> {
    const filters = this.buildRuleFilters();
    const ruleManager = new RuleManager();
    // It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
    // allow to boil over.
    // TODO: Once we know what the output should look like, process the output in some way.
    let output = await ruleManager.runRulesMatchingCriteria(filters);
    return {};
  }

  private buildRuleFilters() : RuleFilter[] {
    let filters : RuleFilter[] = [];
    // Create a filter for any provided categories.
    if ((this.flags.category || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.CATEGORY, this.flags.category));
    }

    // Create a filter for any provided rulesets.
    if ((this.flags.ruleset || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.RULESET, this.flags.ruleset));
    }

    // Create a filter for any provided languages.
    if ((this.flags.language || []).length > 0) {
      filters.push(new RuleFilter(RULE_FILTER_TYPE.LANGUAGE, this.flags.language));
    }

    return filters;
  }
}

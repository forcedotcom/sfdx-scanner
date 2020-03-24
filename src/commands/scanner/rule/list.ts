import {flags} from '@salesforce/command';
import {Messages} from '@salesforce/core';
import {RuleManager} from '../../../lib/RuleManager';
import {Rule} from '../../../types';
import {ScannerCommand} from '../scannerCommand';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'list');
const columns = ['name', 'languages', 'categories', 'rulesets'];

export default class List extends ScannerCommand {
  // These determine what's displayed when the --help/-h flag is supplied.
  public static description = messages.getMessage('commandDescription');
  // TODO: WRITE LEGITIMATE EXAMPLES.
  public static examples = [
    `$ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
  Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
  My hub org id is: 00Dxx000000001234
  `,
    `$ sfdx hello:org --name myname --targetusername myOrg@example.com
  Hello myname! This is org: MyOrg and I will be around until Tue Mar 20 2018!
  `
  ];

  public static args = [{name: 'file'}];

  // This defines the flags accepted by this command. The key is the longname, the char property is the shortname, and description
  // is what's printed when the -h/--help flag is supplied.
  protected static flagsConfig = {
    verbose: flags.builtin(),
    category: flags.array({
      char: 'c',
      description: messages.getMessage('flags.categoryDescription')
    }),
    ruleset: flags.array({
      char: 'r',
      description: messages.getMessage('flags.rulesetDescription')
    }),
    language: flags.array({
      char: 'l',
      description: messages.getMessage('flags.languageDescription')
    }),
    // TODO: After implementing this flag, unhide it.
    severity: flags.string({
      char: 's',
      description: messages.getMessage('flags.severityDescription'),
      hidden: true
    }),
    // TODO: After implementing this flag, unhide it.
    standard: flags.boolean({
      description: messages.getMessage('flags.standardDescription'),
      exclusive: ['custom'],
      hidden: true
    }),
    // TODO: After implementing this flag, unhide it.
    custom: flags.boolean({
      description: messages.getMessage('flags.customDescription'),
      exclusive: ['standard'],
      hidden: true
    })
  };

  public async run(): Promise<Rule[]> {
    const ruleFilters = this.buildRuleFilters();
    // It's possible for this line to throw an error, but that's fine because the error will be an SfdxError that we can
    // allow to boil over.
    const rules = await new RuleManager().getRulesMatchingCriteria(ruleFilters);
    const formattedRules = this.formatRulesForDisplay(rules);
    this.ux.table(formattedRules, columns);
    // If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
    // just return the filtered list instead of the formatted list.
    return rules;
  }

  private formatRulesForDisplay(rules: Rule[]): Rule[] {
    return rules.map(rule => {
      const clonedRule = JSON.parse(JSON.stringify(rule));

      // If any of the rule's rulesets have a name longer than 20 characters, we'll truncate it to 15 and append ellipses,
      // so it doesn't overflow horizontally.
      clonedRule.rulesets = rule['rulesets'].map(ruleset =>
        ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset);
      return clonedRule;
    });
  }
}

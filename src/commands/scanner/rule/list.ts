import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import { PmdCatalogWrapper } from '../../../lib/pmd/PmdCatalogWrapper';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'list');
const columns = ['name', 'languages', 'categories', 'rulesets', 'author'];

enum AuthorFilter {
  All = 1,
  StandardOnly,
  CustomOnly
}
export default class List extends SfdxCommand {
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
    language: flags.array({
      char: 'l',
      description: messages.getMessage('flags.languageDescription')
    }),
    standard: flags.boolean({
      description: messages.getMessage('flags.standardDescription'),
      exclusive: ['custom']
    }),
    custom: flags.boolean({
      description: messages.getMessage('flags.customDescription'),
      exclusive: ['standard']
    })
  };

  public async run() : Promise<AnyJson> {
    try {
      const allRules = await this.getAllRules();
      const filteredRules = allRules.filter((rule) => {return this.ruleSatisfiesFilterConstraints(rule);});
      const formattedRules = this.formatRulesForDisplay(filteredRules);
      this.ux.table(formattedRules, columns);
      // If the --json flag was used, we need to return a JSON. Since we don't have to worry about displayability, we can
      // just return the filtered list instead of the formatted list.
      return filteredRules;
    } catch (err) {
      throw new SfdxError(err);
    }
  }

  private async getAllRules() : Promise<any> {
    // TODO: Eventually, we'll need a bunch more promises to load rules from their source files in other engines.
    const [pmdRules] : AnyJson[] = await Promise.all([this.getPmdRules()]);
    return [...pmdRules];
  }

  private async getPmdRules() : Promise<AnyJson[]> {
    // PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
    const catalog = await new PmdCatalogWrapper().getCatalog();
    return catalog.rules;
  }

  private ruleSatisfiesFilterConstraints(rule : {categories; rulesets; languages}) : boolean {
    // Get the filter criteria from the input flags.
    const filterCats = this.flags.category || [];
    const filterRulesets =  this.flags.ruleset || [];
    const filterLangs = this.flags.language || [];

    // If the user specified one or more categories, this rule must be a member of at least one of those categories.
    if (filterCats.length > 0 && !this.listContentsOverlap(filterCats, rule.categories)) {
      return false;
    }

    // If the user specified one or more rulesets, this rule must be a member of at least one of those rulesets.
    if (filterRulesets.length > 0 && !this.listContentsOverlap(filterRulesets, rule.rulesets)) {
      return false;
    }

    // If the user specified one or more languages, this rule must apply to at least one of those languages.
    if (filterLangs.length > 0 && ! this.listContentsOverlap(filterLangs, rule.languages)) {
      return false;
    }

    // If we didn't find any reasons to disqualify the rule, it's good.
    return true;
  }

  private listContentsOverlap(list1 : string[], list2 : string[]) : boolean {
    return list1.some((x) => {return list2.includes(x)});
  }

  private formatRulesForDisplay(rules : AnyJson[]) : AnyJson[] {
    return rules.map((rule) => {
      const clonedRule = JSON.parse(JSON.stringify(rule));

      // If any of the rule's rulesets have a name longer than 20 characters, we'll truncate it to 15 and append ellipses,
      // so it doesn't overflow horizonatally.
      clonedRule.rulesets = rule.rulesets.map((ruleset) => {
        return ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset;
      });
      return clonedRule;
    });
  }
}

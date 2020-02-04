import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import PmdCatalogWrapper from '../../../lib/pmd/PmdCatalogWrapper';
import {filter} from "minimatch";

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
    // We'll have a bunch of promises to load rules from their source files.
    let pmdPromise = this.getPmdRules();

    // Once all of those promises are done, we'll filter all of the results.
    return Promise.all([pmdPromise])
      .then((res : any[]) => {
        let filteredPmdRules = res[0].filter((rule) => {return this.ruleSatisfiesFilterConstraints(rule);});
        this.ux.table(filteredPmdRules, columns);
        return filteredPmdRules;
      }, (rej : string) => {
        throw new SfdxError(rej);
      })
  }

  private async getPmdRules() : Promise<AnyJson> {
    // PmdCatalogWrapper is a layer of abstraction between the plugin and PMD, facilitating code reuse and other goodness.
    return new PmdCatalogWrapper().getCatalog()
      .then((catalog : {rules}) => {
        // The catalog has a bunch of information on it, but we only need the 'rules' property.
        const rules = catalog.rules;
        // We'll want to do some light tampering with the JSON in order to make sure it displays cleanly on the list.
        return rules.map((rule : {rulesets : string[]}) => {
          // If any of the rulesets have a name longer than 20 characters, we'll truncate it to 15 and append ellipses,
          // so it doesn't overflow horizontally.
          let truncatedRulesets = rule.rulesets.map((ruleset : string) => {
            return ruleset.length >= 20 ? ruleset.slice(0, 15) + '...' : ruleset;
          });
          const cleanedRule = JSON.parse(JSON.stringify(rule));
          cleanedRule.rulesets = truncatedRulesets;
          return cleanedRule;
        });
      });
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
    let matchFound = false;
    for (let i = 0; i < list1.length && !matchFound; i++) {
      matchFound = list2.includes(list1[i]);
    }
    return matchFound;
  }
}

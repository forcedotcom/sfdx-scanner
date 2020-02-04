import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import PmdCatalogWrapper from '../../../lib/pmd/PmdCatalogWrapper';

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
    // Get the filter criteria from the input flags.
    const cats = this.flags.category;
    const rulesets = this.flags.ruleset;
    const sev = this.flags.severity;
    const langs = this.flags.language;
    const author = this.flags.standard ? AuthorFilter.StandardOnly : this.flags.custom ? AuthorFilter.CustomOnly : AuthorFilter.All;

    let pmdPromise = this.getPmdRules();
    return Promise.all([pmdPromise])
      .then((res : any[]) => {
        this.ux.table(res[0], columns);
        return {};
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
}

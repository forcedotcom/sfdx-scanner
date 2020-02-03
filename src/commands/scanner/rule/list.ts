import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import PmdCatalogWrapper from '../../../lib/pmd/PmdCatalogWrapper';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'list');

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

  /**
   * Get high-level information about the rules that match the provided filter criteria.
   * @param {string[]|null} cats - If non-null and non-empty, only rules with a matching category tag will be returned.
   * @param {string[]|null} rulesets - If non-null and non-empty, only rules in the given ruleset will be returned.
   * @param {string|null} sev - If non-null, only rules of the specified severity will be returned.
   * @param {string[]|null} langs - If non-null and non-empty, only rules targeting the specified languages will be returned.
   * @param {AuthorFilter} author - Only rules authored by the specified author will be returned.
   * @returns {Promise<AnyJson[]|string>} Resolves to a list of rules, or rejects with an error message.
   * @private
   */
  private async getRules(cats : string[], rulesets : string[], sev : string, langs : string[], author : AuthorFilter) : Promise<AnyJson[]|string> {
    let rules = [
      {
        name: 'Rule 1',
        categories: ['Best Practice', 'Code Styling'],
        rulesets: ['core/best-practice'],
        languages: ['JS', 'Apex', 'Java'],
        author: 'Salesforce'
      },
      {
        name: 'Rule 2',
        categories: ['Security', 'XSS'],
        rulesets: ['lib/security'],
        languages: ['JS'],
        author: 'Doofenshmirtz Evil Inc'
      }
    ];
    return new Promise((res, rej) => {
      setTimeout(() => {
        res(rules);
      }, 2500);
    });
  }

  private async getPmdRules() : Promise<AnyJson> {
    // We'll use a PmdCatalogWrapper object as a layer of abstraction between our engine and PMD, so declare that now.
    const catalogWrapper = new PmdCatalogWrapper();

    // Check whether the catalog needs to be rebuilt, and do so if needed.
    if (catalogWrapper.catalogIsStale()) {
      try {
        await catalogWrapper.rebuildCatalog();
        return Promise.resolve({});
      } catch (e) {
        this.ux.error('uxerr: ' + (e.message || e));
        return Promise.resolve({});
      }
    }
  }

  public async run(): Promise<AnyJson> {
    await this.getPmdRules();
    const cats = this.flags.category;
    const rulesets = this.flags.ruleset;
    const sev = this.flags.severity;
    const langs = this.flags.language;
    const author = this.flags.standard ? AuthorFilter.StandardOnly : this.flags.custom ? AuthorFilter.CustomOnly : AuthorFilter.All;

    // Since loading the rules might take a while, log something at the start so the user doesn't think we're hanging.
    this.ux.log(messages.getMessage('outputTemplates.preparing'));

    return this.getRules(cats, rulesets, sev, langs, author)
      .then((res : AnyJson[]) => {
        this.ux.table(res, ['name', 'categories', 'rulesets', 'languages', 'author']);
        // This JSON is displayed when the --json flag is provided.
        // TODO: The shape of this JSON will need to change.
        return res;
      }, (rej : string) => {
        return {};
      });
  }
}

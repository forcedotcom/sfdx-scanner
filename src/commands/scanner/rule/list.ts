import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

enum AuthorFilter {
  All = 1,
  StandardOnly,
  CustomOnly
}

enum ActivationFilter {
  All = 1,
  ActiveOnly,
  InactiveOnly
}

export default class List extends SfdxCommand {
  // These determine what's displayed when the --help/-h flag is supplied.
  public static description = messages.getMessage('list.commandDescription');
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
    type: flags.string({
      char: 't',
      description: messages.getMessage('list.flags.typeDescription')
    }),
    severity: flags.string({
      char: 's',
      description: messages.getMessage('list.flags.severityDescription')
    }),
    languages: flags.array({
      char: 'l',
      description: messages.getMessage('list.flags.languagesDescription')
    }),
    standard: flags.boolean({
      char: 'd',
      description: messages.getMessage('list.flags.standardDescription'),
      exclusive: ['custom']
    }),
    custom: flags.boolean({
      char: 'c',
      description: messages.getMessage('list.flags.customDescription'),
      exclusive: ['standard']
    }),
    active: flags.boolean({
      char: 'a',
      description: messages.getMessage('list.flags.activeDescription'),
      exclusive: ['inactive']
    }),
    inactive: flags.boolean({
      char: 'i',
      description: messages.getMessage('list.flags.inactiveDescription'),
      exclusive: ['active']
    }),
  };

  /**
   * Get high-level information about the rules that match the provided filter criteria.
   * @param {string|null} type - If non-null, only rules of the specified type will be returned.
   * @param {string|null} sev - If non-null, only rules of the specified severity will be returned.
   * @param {string[]|null} langs - If non-null and non-empty, only rules targeting the specified languages will be returned.
   * @param {AuthorFilter} author - Only rules authored by the specified author will be returned.
   * @param {ActivationFilter} activation - Only rules with the specified activation status will be returned.
   * @returns {Promise<AnyJson[]|string>} Resolves to a list of rules, or rejects with an error message.
   * @private
   */
  private async getRules(type : string, sev : string, langs : string[], author : AuthorFilter, activation : ActivationFilter) : Promise<AnyJson[]|string> {
    let rules = [
      {
        name: 'Rule 1',
        type: 'Security',
        languages: ['JS', 'Apex', 'Java'],
        author: 'Salesforce',
        active: 'Active'
      },
      {
        name: 'Rule 2',
        type: 'Best Practice',
        languages: ['JS'],
        author: 'Doofenshmirtz Evil Inc',
        active: 'Inactive'
      }
    ];
    return new Promise((res, rej) => {
      setTimeout(() => {
        res(rules);
      }, 2500);
    });
  }

  public async run(): Promise<AnyJson> {
    const type = this.flags.type;
    const sev = this.flags.severity;
    const langs = this.flags.languages;
    const author = this.flags.standard ? AuthorFilter.StandardOnly : this.flags.custom ? AuthorFilter.CustomOnly : AuthorFilter.All;
    const activation = this.flags.active ? ActivationFilter.ActiveOnly : this.flags.inactive ? ActivationFilter.InactiveOnly : ActivationFilter.All;

    // Since loading the rules might take a while, log something at the start so the user doesn't think we're hanging.
    this.ux.log(messages.getMessage('list.outputTemplates.preparing'));

    return this.getRules(type, sev, langs, author, activation)
      .then((res : AnyJson[]) => {
        this.ux.table(res, ['name', 'type', 'languages', 'author', 'active']);
        // This JSON is displayed when the --json flag is provided.
        // TODO: The shape of this JSON will need to change.
        return res;
      }, (rej : string) => {
        return {};
      });
  }
}

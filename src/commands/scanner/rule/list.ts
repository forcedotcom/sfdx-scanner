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

  public static description = messages.getMessage('list.commandDescription');

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

  protected static flagsConfig = {
    // flag with a value (-n, --name=VALUE)
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
      description: messages.getMessage('list.flags.standardDescription')
    }),
    custom: flags.boolean({
      char: 'c',
      description: messages.getMessage('list.flags.customDescription')
    }),
    active: flags.boolean({
      char: 'a',
      description: messages.getMessage('list.flags.activeDescription')
    }),
    inactive: flags.boolean({
      char: 'i',
      description: messages.getMessage('list.flags.inactiveDescription')
    }),
  };

  private async getRules(type : string, sev : string, langs : string[], author : AuthorFilter, activation : ActivationFilter) : Promise<Object[]> {
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

  private validateFlags() : void {
    if (this.flags.standard && this.flags.custom) {
      throw Error(messages.getMessage('list.flagValidations.authorFlagsMutex'));
    }
    if (this.flags.active && this.flags.inactive) {
      throw Error(messages.getMessage('list.flagValidations.activationFlagsMutex'));
    }
  }

  public async run(): Promise<AnyJson> {
    this.validateFlags();

    const type = this.flags.type;
    const sev = this.flags.severity;
    const langs = this.flags.languages;
    const author = this.flags.standard ? AuthorFilter.StandardOnly : this.flags.custom ? AuthorFilter.CustomOnly : AuthorFilter.All;
    const activation = this.flags.active ? ActivationFilter.ActiveOnly : this.flags.inactive ? ActivationFilter.InactiveOnly : ActivationFilter.All;

    return this.getRules(type, sev, langs, author, activation)
      .then((res : Object[]) => {
        this.ux.table(res, ['name', 'type', 'languages', 'author', 'active']);
        return {};
      }, (rej) => {
        return {};
      });
  }
}

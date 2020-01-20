import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

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

  public async run(): Promise<AnyJson> {
    return {};
  }
}

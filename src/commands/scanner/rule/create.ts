import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

export default class Create extends SfdxCommand {

  public static description = messages.getMessage('create.commandDescription');

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
    rulename: flags.string({
      char: 'n',
      description: messages.getMessage('create.flags.rulenameDescription'),
      required: true
    }),
    type: flags.string({
      char: 't',
      description: messages.getMessage('create.flags.typeDescription')
    }),
    severity: flags.string({
      char: 's',
      description: messages.getMessage('create.flags.severityDescription')
    }),
    languages: flags.array({
      char: 'l',
      description: messages.getMessage('create.flags.languagesDescription')
    }),
    original: flags.filepath({
      char: 'o',
      description: messages.getMessage('create.flags.originalDescription')
    })
  };

  public async run(): Promise<AnyJson> {
    return {};
  }
}

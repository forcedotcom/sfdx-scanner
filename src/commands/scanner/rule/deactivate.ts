import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

export default class Deactivate extends SfdxCommand {

  public static description = messages.getMessage('deactivate.commandDescription');

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
      description: messages.getMessage('deactivate.flags.rulenameDescription'),
      required: true
    })
  };

  private performDeactivation(name: String) : Promise<boolean> {
    return new Promise((res, rej) => {
      setTimeout(() => {
        res(name === 'passingval');
      }, 2500);
    });
  }

  public async run(): Promise<AnyJson> {
    const rulename = this.flags.rulename;
    this.ux.log("Preparing to deactivate this rule: '" + rulename + "'. Here's hoping that works out for you.");

    let ruleState;
    if (await this.performDeactivation(rulename)) {
      this.ux.log("Successfully deactivated rule: '" + rulename + "'. I'm glad that worked out so well for you.");
      ruleState = 'inactive';
    } else {
      this.ux.log("Failed to deactivate rule: '" + rulename + "'. Sucks to suck.");
      ruleState = 'active';
    }
    return {rulestate: ruleState};
  }
}

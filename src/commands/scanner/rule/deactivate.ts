import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

enum DeactivationResult {
  Success = 1,
  NoSuchRule,
  AlreadyInactive,
  OtherFailure
}

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

  private buildOutputString(name : string, status : DeactivationResult) : string {
    let msgTemplate : string;
    switch (status) {
      case DeactivationResult.Success:
        msgTemplate = messages.getMessage('deactivate.outputTemplates.success');
        break;
      case DeactivationResult.NoSuchRule:
        msgTemplate = messages.getMessage('deactivate.outputTemplates.nosuchrule');
        break;
      case DeactivationResult.AlreadyInactive:
        msgTemplate = messages.getMessage('deactivate.outputTemplates.alreadyactive');
        break;
      default:
        msgTemplate = messages.getMessage('deactivate.outputTemplates.otherfailure');
    }
    return msgTemplate.replace('{0}', name);
  }

  private performDeactivation(name: String) : Promise<DeactivationResult> {
    return new Promise((res, rej) => {
      setTimeout(() => {
        if (name === 'valid-rule') {
          res(DeactivationResult.Success);
        } else if (name === 'non-existent-rule') {
          rej(DeactivationResult.NoSuchRule);
        } else if (name === 'already-inactive-rule') {
          rej(DeactivationResult.AlreadyActive);
        } else {
          rej(DeactivationResult.OtherFailure);
        }
      }, 2500);
    });
  }

  public async run(): Promise<AnyJson> {
    const rulename = this.flags.rulename;
    this.ux.log(messages.getMessage('deactivate.outputTemplates.preparing').replace('{0}', rulename));
    return this.performDeactivation(rulename)
      .then((state: DeactivationResult) => {
        this.ux.log(this.buildOutputString(rulename, state));
        return {rulestate: state};
      }, (state: DeactivationResult) => {
        throw Error(this.buildOutputString(rulename, state));
      });
  }
}

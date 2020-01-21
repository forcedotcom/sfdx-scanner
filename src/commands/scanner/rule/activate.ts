import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'rule');

enum ActivationResult {
  Success = 1,
  NoSuchRule,
  AlreadyActive,
  OtherFailure
}

export default class Activate extends SfdxCommand {
  // These determine what is printed when the -h/--help flag is supplied.
  public static description = messages.getMessage('activate.commandDescription');
  // TODO: REWRITE THESE EXAMPLES TO BE LEGITIMATE.
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
    rulename: flags.string({
      char: 'n',
      description: messages.getMessage('activate.flags.rulenameDescription'),
      required: true
    })
  };

  /**
   * Constructs the string logged to the console.
   * @param {string} name - The name of the rule being targeted.
   * @param {ActivationResult} status - The result of the action.
   * @returns {string} - The message describing the outcome.
   * @private
   */
  private buildOutputString(name : string, status : ActivationResult) : string {
    let msgTemplate : string;
    switch (status) {
      case ActivationResult.Success:
        msgTemplate = messages.getMessage('activate.outputTemplates.success');
        break;
      case ActivationResult.NoSuchRule:
        msgTemplate = messages.getMessage('activate.outputTemplates.nosuchrule');
        break;
      case ActivationResult.AlreadyActive:
        msgTemplate = messages.getMessage('activate.outputTemplates.alreadyactive');
        break;
      default:
        msgTemplate = messages.getMessage('activate.outputTemplates.otherfailure');
    }
    // By convention, we'll wrap wildcards in brackets and number them starting at 0.
    return msgTemplate.replace('{0}', name);
  }

  /**
   * Activates the specified rule.
   * @param {string} name - The name of the rule being targeted.
   * @returns {Promise<ActivationResult>} Resolves to a successful result or rejects with a failing result.
   * @private
   */
  private async performActivation(name: string) : Promise<ActivationResult> {
    return new Promise((res, rej) => {
      // TODO: This is dummy code that just waits 2.5 seconds before resolving to one of four results based on the input.
      //       Replace it with real code.
      setTimeout(() => {
        if (name === 'valid-rule') {
          res(ActivationResult.Success);
        } else if (name === 'non-existent-rule') {
          rej(ActivationResult.NoSuchRule);
        } else if (name === 'already-active-rule') {
          rej(ActivationResult.AlreadyActive);
        } else {
          rej(ActivationResult.OtherFailure);
        }
      }, 2500);
    });
  }

  public async run(): Promise<AnyJson> {
    const rulename = this.flags.rulename;

    this.ux.log(messages.getMessage('activate.outputTemplates.preparing').replace('{0}', rulename));
    return this.performActivation(rulename)
      .then((state: ActivationResult) => {
        this.ux.log(this.buildOutputString(rulename, state));
        // The returned JSON is used if the universally-supported --json parameter was supplied.
        // TODO: The exact form of this JSON will probably need to change.
        return {rulestate: state};
      }, (state: ActivationResult) => {
        throw Error(this.buildOutputString(rulename, state));
      });
  }
}

import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import { CustomRulePathManager } from '../../../lib/CustomRulePathManager';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'add');


export default class Add extends SfdxCommand {

  public static description = messages.getMessage('commandDescription');

  public static examples = [
    messages.getMessage('examples')
  ];

  protected static flagsConfig = {
    language: flags.string({
        char: 'l',
        description: messages.getMessage('flags.languageDescription'),
        required: true
    }),
    path: flags.array({
        char: 'p',
        description: messages.getMessage('flags.pathDescription'),
        required: true
    })
  };

  public async run(): Promise<AnyJson> {
    this.validateFlags();

    const language = this.flags.language;
    const path = this.flags.path;

    this.logger.trace(`Language: ${language}`);
    this.logger.trace(`Rule path: ${path}`);

    // Add to Custom Classpath registry
    const manager = new CustomRulePathManager();
    const classpathEntries = await manager.addPathsForLanguage(language, path);
    this.ux.log(`Successfully added rules for ${language}.`);
    this.ux.log(`${classpathEntries.length} Path(s) added: ${classpathEntries}`);
    return { success: true, language, path: classpathEntries };
  }

  private validateFlags(): void {
    if (this.flags.language.length === 0) {
      throw SfdxError.create('@salesforce/sfdx-scanner', 'add', 'validations.languageCannotBeEmpty', []);
    }
    if (this.flags.path.includes('')) {
      throw SfdxError.create('@salesforce/sfdx-scanner', 'add', 'validations.pathCannotBeEmpty', []);
    }
  }

}

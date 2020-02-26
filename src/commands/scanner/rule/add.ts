import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { AnyJson } from '@salesforce/ts-types';
import { CustomClasspathManager } from '../../../lib/CustomClasspathManager';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'add');


export default class Add extends SfdxCommand {

  public static description = messages.getMessage('commandDescription');

  public static examples = [
    `$ sfdx scanner:rule:add --language "apex" --path "/dir/to/jar/lib"
        (todo: add sample output here)
        $ sfdx scanner:rule:add --language "apex" --path "/file/path/to/customrule.jar,/dir/to/jar/lib"
        (todo: add sample output here)
        `
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
    const paths = this.flags.path;

    this.logger.trace(`Language: ${language}`);
    this.logger.trace(`Rule path: ${paths}`);

    // Add to Custom Classpath registry
    const creator = new CustomClasspathManager();
    await creator.createEntries(language, paths);

    return { success: true, language: language, paths: paths };
  }

  private validateFlags() {
    if (this.flags.language.length === 0) {
      throw SfdxError.create('scanner', 'add', 'validations.languageCannotBeEmpty', []);
    }
    if (this.flags.path.includes('')) {
      throw SfdxError.create('scanner', 'add', 'validations.pathCannotBeEmpty', []);
    }
  }

}

import { flags, SfdxCommand } from '@salesforce/command';
import { Messages, SfdxError } from '@salesforce/core';
import { CustomClasspathRegistrar } from '../../../lib/customclasspath/CustomClasspathRegistrar';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'add');


export default class Addcustom extends SfdxCommand {

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
        description: messages.getMessage('flags.languageFlagDescription'),
        required: true
    }),
    path: flags.string({
        char: 'p',
        description: messages.getMessage('flags.pathFlagDescription'),
        required: true
    })
  };

  public async run(): Promise<any> {

    if (this.flags.language.length === 0) {
      throw SfdxError.create('scanner', 'add', 'validations.errorLanguageCannotBeEmpty', []);
    }

    if (this.flags.path.length === 0) {
      throw SfdxError.create('scanner', 'add', 'validations.errorPathCannotBeEmpty', []);
    }

    const language = this.flags.language;
    const path = this.breakCommaSeparatedString(this.flags.path);


    this.ux.log(`Language: ${language}`);
    this.ux.log(`Rule path: ${path}`);


    this.ux.log('Adding to mapping');
    const creator = new CustomClasspathRegistrar();
    await creator.createEntries(language, path);

    return; // TODO: fill in return json
  }

  breakCommaSeparatedString(pathString: string): string[] {
    const tempArray = pathString.split(',');
    const path = [];
    tempArray.forEach(item => {
      const trimmedValue = item.trim();
      if (trimmedValue.length > 0) {
        path.push(item.trim());
      }

    });

    return path;
  }

}

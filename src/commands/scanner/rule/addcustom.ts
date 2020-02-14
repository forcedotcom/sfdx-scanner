import { flags, SfdxCommand } from '@salesforce/command';
import { Messages } from '@salesforce/core';
import { LanguageMappingCreator } from '../../../lib/pmd/LanguageMappingCreator';


// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'addcustom');


export default class Addcustom extends SfdxCommand {

  public static description = messages.getMessage('commandDescription');

  public static examples = [
    `$ sfdx scanner:rule:addcustom --language "apex" --libdir "/dir/to/jar/lib"
        (todo: add sample output here)
        
        $ sfdx scanner:rule:addcustom --language "apex" --jar "/file/path/to/customrule.jar"
        (todo: add sample output here)

        $ sfdx scanner:rule:addcustom --language "apex" --classfiles "/file/path/to/compiled/classes/and/xml"
        (todo: add sample output here)
        `
  ];

  protected static flagsConfig = {
    language: flags.string({ 
        char: 'l', 
        description: messages.getMessage('languageFlagDescription'),
        required: true
    }),
    libdir: flags.string({ 
        description: messages.getMessage('libdirFlagDescription') 
    }),
    jar: flags.string({ 
        description: messages.getMessage('jarFlagDescription') 
    }),
    classfile: flags.string({ 
        description: messages.getMessage('classfileFlagDescription')
    })
  };

  public async run(): Promise<any> {
    const language = this.flags.language;

    // TODO: handle all other types
    const jarFile = this.flags.jar;


    this.ux.log(`Language: ${language}`);
    this.ux.log(`Jar file: ${jarFile}`);


    this.ux.log('Adding to mapping');
    const creator = new LanguageMappingCreator();
    await creator.createMapping(language, [jarFile]);


    return;
  }
}

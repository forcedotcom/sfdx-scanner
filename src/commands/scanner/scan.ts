import { flags, SfdxCommand } from '@salesforce/command';
import { Messages } from '@salesforce/core';

import PMD, { Format } from '../../../src/lib/pmd/pmd';

// import { AnyJson } from '@salesforce/ts-types';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

// Load the specific messages for this file. Messages from @salesforce/command, @salesforce/core,
// or any library that is using the messages framework can also be loaded this way.
const messages = Messages.loadMessages('scanner', 'scan');


export default class Scan extends SfdxCommand {

  public static description = messages.getMessage('commandDescription');

  public static examples = [
    `$ sfdx scanner:scan --ruleset "/my/ruleset/file/location" --filepath "/my/code/files/to/be/scanned"
        (todo: add sample output here)

        $ sfdx scanner:scan -R "/my/ruleset/file/location" -d "/my/code/files/to/be/scanned"
        (todo: add sample output here)
        `
  ];

  protected static flagsConfig = {
    ruleset: flags.string({ char: 'R', description: messages.getMessage('rulesetFlagDescription') }),
    filepath: flags.string({ char: 'd', description: messages.getMessage('filepathFlagDescription') })
  };

  public async run(): Promise<any> {
    const rulesetFiles = this.flags.ruleset;
    const filepathName = this.flags.filepath;


    this.ux.log(`Location of ruleset is ${rulesetFiles}`);
    this.ux.log(`File path is ${filepathName}`);

    this.ux.log('Handing off to PMD');
    PMD.execute(filepathName, rulesetFiles, Format.JSON);
    this.ux.log('Completed PMD execution');

    return { ruleset: rulesetFiles, filepath: filepathName };
  }
}
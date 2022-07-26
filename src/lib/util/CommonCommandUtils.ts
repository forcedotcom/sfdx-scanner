import { SfdxCommand } from "@salesforce/command";
import {Messages} from '@salesforce/core';

// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);
const commonMessages = Messages.loadMessages('@salesforce/sfdx-scanner', 'common');




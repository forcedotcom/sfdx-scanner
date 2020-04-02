/**
 *  Code to be evaluated after the scanner plugin is installed.
 */

const JreSetupManager = require('./JreSetupManager');
const messages = require('../../messages/postinstall');
import {UX} from '@salesforce/command';


export async function execute(): Promise<void> {
  const ux: UX = await UX.create();
  ux.startSpinner(messages.startingAll);
  // Run our scripts in a try-catch block so any important errors can bubble up to the top and be logged.
  try {
    // First, verify that Java is setup properly.
    await verifyJava(ux);
    // If we're here, there weren't any fatal errors, though there might have been some warnings.
    ux.stopSpinner(messages.done);
  } catch (e) {
    ux.error(messages.errorTemplate.replace('%s', e.message || e));
    ux.stopSpinner(messages.failed);
  }
}


async function verifyJava(ux: UX): Promise<void> {
  ux.setSpinnerStatus(messages.verifyingJava);
  // Use a try-catch block so we can surface issues as non-fatal warnings instead of errors.
  try {
    await JreSetupManager.verifyJreSetup();
  } catch (e) {
    ux.warn(e.message || e);
  }
}

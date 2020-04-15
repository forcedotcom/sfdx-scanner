import {expect, test} from '@salesforce/command/lib/test';
import {Messages} from '@salesforce/core';
import * as os from 'os';
import {SFDX_SCANNER_PATH} from '../../../../src/Constants';
import fs = require('fs');
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'remove');

const CATALOG_OVERRIDE = 'RemoveTestPmdCatalog.json';
const CUSTOM_PATH_OVERRIDE = 'RemoveTestCustomPaths.json';

// Delete any existing JSONs associated with the tests so they run fresh each time.
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE));
}
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE))) {
	fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE));
}

let removeTest = test.env({PMD_CATALOG_NAME: CATALOG_OVERRIDE, CUSTOM_PATH_FILE: CUSTOM_PATH_OVERRIDE});

describe('scanner:rule:remove', () => {
	describe('E2E', () => {
		describe('Test Case: Removing a single JAR', () => {

		});

		describe('Test Case: Removing multiple JARs', () => {

		});

		describe('Test Case: Removing an entire folder', () => {

		});


	});
});

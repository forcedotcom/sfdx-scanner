import { expect, test } from '@salesforce/command/lib/test';
import { Messages } from '@salesforce/core';
import * as os from 'os';
import { SFDX_SCANNER_PATH } from '../../../../src/Constants';
import fs = require('fs');
import path = require('path');


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'add');


describe('scanner:rule:add', () => {
  describe('E2E', () => {
    describe('Happy Path', () => {
      const CATALOG_OVERRIDE = 'AddTestPmdCatalog.json';
      const CUSTOM_PATH_OVERRIDE = 'AddTestCustomPaths.json';

      // Delete any existing JSONs associated with the test so it runs fresh each time.
      if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE))) {
        fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE));
      }
      if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE))) {
        fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE));
      }

      const myLanguage = 'apex';

      // Use two dirs, with one jar file in each.
      const tmpDir1 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
      const tmpFile1 = path.join(tmpDir1, 'bar.jar');
      fs.writeFileSync(tmpFile1, "There is no spoon");

      const tmpDir2 = fs.mkdtempSync(path.join(os.tmpdir(), 'bar-'));
      const tmpFile2 = path.join(tmpDir2, 'foo.jar');
      fs.writeFileSync(tmpFile2, "Spoon is no there");

      // Now pass the first dir and the second file.
      const myPath = [tmpDir1, tmpFile2];

      // Result should be three entries: dir 1, file 1, and file 2
      const expectedPath = [tmpDir1, tmpFile1, tmpFile2];
      test
        .env({PMD_CATALOG_NAME: CATALOG_OVERRIDE, CUSTOM_PATH_FILE: CUSTOM_PATH_OVERRIDE})
        .stdout()
        .stderr()
        .command(['scanner:rule:add', '--language', myLanguage, '--path', myPath.join(','), '--json'])
        .it('should run successfully and add entries to custom classpath json', ctx => {
          const outputJson = JSON.parse(ctx.stdout);
          const result = outputJson.result;
          expect(result).to.have.property('success')
            .and.equals(true);

          expect(result).to.have.property('language')
            .and.equals(myLanguage);

          expect(result).to.have.property('path')
            .and.have.lengthOf(expectedPath.length)
            .and.deep.equals(expectedPath);
        });
    });
  });

  describe('Validations', () => {
    describe('Language validations', () => {
      // Test for failure scenario doesn't need to do any special setup or cleanup.
      test
        .stdout()
        .stderr()
        .command(['scanner:rule:add','--path', '/some/local/path'])
        .it('should complain about missing --language flag', ctx => {
          expect(ctx.stderr).contains(messages.getMessage('flags.languageDescription'));
        });

      // Test for failure scenario doesn't need to do any special setup or cleanup.
      test
        .stdout()
        .stderr()
        .command(['scanner:rule:add', '--language', '', '--path', '/some/local/path'])
        .it('should complain about empty language entry', ctx => {
          expect(ctx.stderr).contains(messages.getMessage('validations.languageCannotBeEmpty'));
        });
    });

    describe('Path validations', () => {
      // Test for failure scenario doesn't need to do any special setup or cleanup.
      test
        .stdout()
        .stderr()
        .command(['scanner:rule:add','--language', 'apex'])
        .it('should complain about missing --path flag', ctx => {
          expect(ctx.stderr).contains('Missing required flag:\n -p, --path PATH');
        });

      // Test for failure scenario doesn't need to do any special setup or cleanup.
      test
        .stdout()
        .stderr()
        .command(['scanner:rule:add', '--language', 'apex', '--path', ''])
        .it('should complain about empty path', ctx => {
          expect(ctx.stderr).contains(messages.getMessage('validations.pathCannotBeEmpty'));
        });
    });
  });
});

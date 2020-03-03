import { expect, test } from '@salesforce/command/lib/test';
import { Messages } from '@salesforce/core';
import { CUSTOM_CLASSPATH_REGISTER } from '../../../../src/lib/CustomRulePathManager';
import fs = require('fs');


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('scanner', 'add');


describe('scanner:rule:add', () => {
    describe('Language related validations', () => {

        test
        .stdout()
        .stderr()
        .command(['scanner:rule:add','--path', '/some/local/path'])
        .it('should complain about missing --language flag', ctx => {
            expect(ctx.stderr).contains(messages.getMessage('flags.languageDescription'));
        });

        test
        .stdout()
        .stderr()
        .command(['scanner:rule:add', '--language', '', '--path', '/some/local/path'])
        .it('should complain about empty language entry', ctx => {
            expect(ctx.stderr).contains(messages.getMessage('validations.languageCannotBeEmpty'));
        });
    });

    describe('Path related validations', () => {

        test
        .stdout()
        .stderr()
        .command(['scanner:rule:add','--language', 'apex'])
        .it('should complain about missing --path flag', ctx => {
            expect(ctx.stderr).contains('Missing required flag:\n -p, --path PATH');
        });

        test
        .stdout()
        .stderr()
        .command(['scanner:rule:add', '--language', 'apex', '--path', ''])
        .it('should complain about empty path', ctx => {
            expect(ctx.stderr).contains(messages.getMessage('validations.pathCannotBeEmpty'));
        });
    });

    // TODO: This test modifies the same JSON as an actual run. Find a way to substitute file names
    // Temporary fix: 
    // 1. Backup contents of actual CustomClasspathRegistry
    // 2. Run test, which would create dummy entries
    // 3. Restore with original file
    describe('Happy case to add custom path', async () => {

        const myLanguage = 'apex';
        const myPath = ['/some/local/path', 'some/other/path'];
        const tmpFile = './.tmpCustom';

        // Backup contents of actual CustomClasspathRegistry
        await fs.promises.rename(CUSTOM_CLASSPATH_REGISTER, tmpFile);

        test
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
            .and.have.lengthOf(myPath.length)
            .and.deep.equals(myPath);
        });

        // Restore with original file
        await fs.promises.rename(tmpFile, CUSTOM_CLASSPATH_REGISTER);
    });
});
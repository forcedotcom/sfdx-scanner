import {expect, test} from '@salesforce/command/lib/test';
import {Messages} from '@salesforce/core';
import * as TestOverrides from '../../../test-related-lib/TestOverrides';
import * as os from 'os';
import fs = require('fs');
import path = require('path');


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'add');

describe('scanner:rule:add', () => {
	// Reset our controller since we are using alternate file locations
	before(() => TestOverrides.initializeTestSetup());

	describe('E2E', () => {
		const myLanguage = 'apex';
		describe('Test Case: Adding individual JARs', () => {
			// Create four temporary directories, each having a single JAR.
			const tmpDir1 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar1 = path.join(tmpDir1, 'bar1.jar');
			fs.writeFileSync(tmpJar1, 'In the Age of Ancients, the world was unformed and shrouded by fog.');
			const tmpDir2 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar2 = path.join(tmpDir2, 'bar2.jar');
			fs.writeFileSync(tmpJar2, 'A land of grey crags, arch trees, and everlasting dragons.');
			const tmpDir3 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar3 = path.join(tmpDir3, 'bar3.jar');
			fs.writeFileSync(tmpJar3, 'Then there was Fire, and with Fire came disparity.');
			const tmpDir4 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar4 = path.join(tmpDir4, 'bar4.jar');
			fs.writeFileSync(tmpJar4, 'Heat and cold, life and death, and of course, light and dark.');

			// For the first test, we'll add two of the JARs with their absolute paths.
			const absolutePaths = [tmpJar1, tmpJar2];
			test
				.stdout()
				.stderr()
				.command(['scanner:rule:add',
					'--language', myLanguage,
					'--path', absolutePaths.join(','),
					'--json'
				])
				.it('Individual JARs can be added with their absolute path', ctx => {
					// The expectation is that the paths will just be used as-is.
					const expectedPaths = absolutePaths;
					const outputJson = JSON.parse(ctx.stdout);
					const result = outputJson.result;
					expect(result).to.have.property('success')
						.and.equals(true);

					expect(result).to.have.property('language')
						.and.equals(myLanguage);

					expect(result).to.have.property('path')
						.and.have.lengthOf(expectedPaths.length)
						.and.deep.equals(expectedPaths);
				});


			// For the second test, we'll add the other two JARs with relative paths.
			const relativePaths = [path.relative('.', tmpJar3), path.relative('.', tmpJar4)];
			test
				.stdout()
				.stderr()
				.command(['scanner:rule:add',
					'--language', myLanguage,
					'--path', relativePaths.join(','),
					'--json'
				])
				.it('Individual JARs can be added with their relative paths', ctx => {
					// The expectation is that the paths will be converted to absolute paths.
					const expectedPaths = [tmpJar3, tmpJar4];
					const outputJson = JSON.parse(ctx.stdout);
					const result = outputJson.result;
					expect(result).to.have.property('success')
						.and.equals(true);

					expect(result).to.have.property('language')
						.and.equals(myLanguage);

					expect(result).to.have.property('path')
						.and.have.lengthOf(expectedPaths.length)
						.and.deep.equals(expectedPaths);
				});
		});


		describe('Test Case: Adding all JARs in a folder', () => {
			// Create two temporary directories, each containing three JARs.
			const tmpDir1 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar1 = path.join(tmpDir1, 'bar1.jar');
			const tmpJar2 = path.join(tmpDir1, 'bar2.jar');
			const tmpJar3 = path.join(tmpDir1, 'bar3.jar');
			fs.writeFileSync(tmpJar1, 'Then from the dark They came, and found the Souls of Lords within the flame.');
			fs.writeFileSync(tmpJar2, 'Gravelord Nito, the First of the Dead.');
			fs.writeFileSync(tmpJar3, 'The Witch of Izaleth, and her Daughters of Chaos.');
			const tmpDir2 = fs.mkdtempSync(path.join(os.tmpdir(), 'foo-'));
			const tmpJar4 = path.join(tmpDir2, 'bar4.jar');
			const tmpJar5 = path.join(tmpDir2, 'bar5.jar');
			const tmpJar6 = path.join(tmpDir2, 'bar6.jar');
			fs.writeFileSync(tmpJar4, 'Gwynn, Lord of Sunlight, and his faithful knights.');
			fs.writeFileSync(tmpJar5, 'And the Furtive Pygmy, so easily forgotten.');
			fs.writeFileSync(tmpJar6, 'With the strength of Lords, they challenged the Dragons.');

			// For the first test, we'll add one of the folders by its absolute path.
			test
				.stdout()
				.stderr()
				.command(['scanner:rule:add',
					'--language', myLanguage,
					'--path', tmpDir1,
					'--json'
				])
				.it('Folders can be added with their absolute paths', ctx => {
					// The expectation is that all three JARs in the folder will have been added.
					const expectedPaths = [tmpJar1, tmpJar2, tmpJar3];
					const outputJson = JSON.parse(ctx.stdout);
					const result = outputJson.result;
					expect(result).to.have.property('success')
						.and.equals(true);

					expect(result).to.have.property('language')
						.and.equals(myLanguage);

					expect(result).to.have.property('path')
						.and.have.lengthOf(expectedPaths.length)
						.and.deep.equals(expectedPaths);
				});

			// For the second test, we'll add the other folder by its relative path.
			test
				.stdout()
				.stderr()
				.command(['scanner:rule:add',
					'--language', myLanguage,
					'--path', path.relative('.', tmpDir2),
					'--json'
				])
				.it('Folders can be added by their relative paths', ctx => {
					// The expectation is that all three JARs in the folder will have been added.
					const expectedPaths = [tmpJar4, tmpJar5, tmpJar6];
					const outputJson = JSON.parse(ctx.stdout);
					const result = outputJson.result;
					expect(result).to.have.property('success')
						.and.equals(true);

					expect(result).to.have.property('language')
						.and.equals(myLanguage);

					expect(result).to.have.property('path')
						.and.have.lengthOf(expectedPaths.length)
						.and.deep.equals(expectedPaths);
				});
		});
	});

	describe('Validations', () => {
		describe('Language validations', () => {
			// Test for failure scenario doesn't need to do any special setup or cleanup.
			test
				.stdout()
				.stderr()
				.command(['scanner:rule:add', '--path', '/some/local/path'])
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
				.command(['scanner:rule:add', '--language', 'apex'])
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

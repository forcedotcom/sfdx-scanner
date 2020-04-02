import { expect, test } from '@salesforce/command/lib/test';
import {Messages} from '@salesforce/core';
import fs = require('fs');
import path = require('path');
import {SFDX_SCANNER_PATH} from '../../../src/Constants';
import events = require('../../../messages/EventKeyTemplates');

const CATALOG_OVERRIDE = 'RunTestPmdCatalog.json';
const CUSTOM_PATH_OVERRIDE = 'RunTestCustomPaths.json';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'run');

// Before our tests, delete any existing catalog and/or custom path associated with our override.
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE))) {
  fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CATALOG_OVERRIDE));
}
if (fs.existsSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE))) {
  fs.unlinkSync(path.join(SFDX_SCANNER_PATH, CUSTOM_PATH_OVERRIDE));
}

let runTest = test.env({PMD_CATALOG_NAME: CATALOG_OVERRIDE, CUSTOM_PATH_FILE: CUSTOM_PATH_OVERRIDE});

describe('scanner:run', () => {
  describe('E2E', () => {
    // XML output is more thoroughly tested than other output types because, at time of writing (2/21/2020), the only engine
    // supported is PMD, whose output is already an XML. So we're really just testing the other formats to make sure that
    // we properly convert the output from an XML.
    describe('Output Type: XML', () => {
      describe('Test Case: Running rules against a single file', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
            '--ruleset', 'ApexUnit',
            '--format', 'xml'
          ])
          .it('When the file contains violations, they are logged out as an XML', ctx => {
            // We'll split the output by the <violation> tag, so we can get individual violations.
            const violations = ctx.stdout.split('<violation');
            // The first list item is going to be the header, so we need to pull that off.
            violations.shift();
            // There should be four violations.
            expect(violations.length).to.equal(4, 'Should be four violations detected in the file');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(violations[0]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[1]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[2]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[3]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);
          });

        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
            '--ruleset', 'ApexUnit',
            '--format', 'xml'
          ])
          .it('When the file contains no violations, a message is logged to the console', ctx => {
            expect(ctx.stdout).to.contain(messages.getMessage('output.noViolationsDetected'));
          });
      });

      describe('Test Case: Running rules against multiple specified files', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls') + ',' + path.join('test', 'code-samples', 'apex', 'InstallProcessorTests.cls'),
            '--ruleset', 'ApexUnit',
            '--format', 'xml'
          ])
          .it('Both files are evaluated, and any violations are logged', ctx => {
            // We'll split the output by the <file> tag first, so we can get each file that violated rules.
            const files = ctx.stdout.split('<file');
            // The first list item is going to be the header, so we need to pull that off.
            files.shift();
            // Verify that each set of violations corresponds to the expected file.
            expect(files.length).to.equal(2, 'Only two files should have violated the rules');
            expect(files[0]).to.match(/name="\S+\/test\/code-samples\/apex\/AccountServiceTests.cls"/);
            expect(files[1]).to.match(/name="\S+\/test\/code-samples\/apex\/InstallProcessorTests.cls"/);

            // Now, split each file's violations by the <violation> tag so we can inspect individual violations.
            const acctServiceViolations = files[0].split('<violation');
            acctServiceViolations.shift();
            // There should be four violations.
            expect(acctServiceViolations.length).to.equal(4, 'Should be four violations detected in AccountServiceTests.cls');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(acctServiceViolations[0]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[1]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[2]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[3]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);

            const installProcessorViolations = files[1].split('<violation');
            installProcessorViolations.shift();
            // There should be one violation.
            expect(installProcessorViolations.length).to.equal(1, 'Should be one violation detected in InstallProcessorTests.cls');
            expect(installProcessorViolations[0]).to.match(/beginline="953".+rule="ApexUnitTestClassShouldHaveAsserts"/);
          });
      });

      describe('Test Case: Running rules against a folder', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex'),
            '--ruleset', 'ApexUnit',
            '--format', 'xml'
          ])
          .it('Any violations in the folder are logged as an XML', ctx => {
            // We'll split the output by the <file> tag first, so we can get each file that violated rules.
            let files = ctx.stdout.split('<file');
            // The first list item is going to be the header, so we need to pull that off.
            files.shift();
            // Verify that each set of violations corresponds to the expected file.
            expect(files.length).to.equal(2, 'Only two files should have violated the rules');
            expect(files[0]).to.match(/name="\S+\/test\/code-samples\/apex\/AccountServiceTests.cls"/);
            expect(files[1]).to.match(/name="\S+\/test\/code-samples\/apex\/InstallProcessorTests.cls"/);

            // Now, split each file's violations by the <violation> tag so we can inspect individual violations.
            const acctServiceViolations = files[0].split('<violation');
            acctServiceViolations.shift();
            // There should be four violations.
            expect(acctServiceViolations.length).to.equal(4, 'Should be four violations detected in AccountServiceTests.cls');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(acctServiceViolations[0]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[1]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[2]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[3]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);

            let installProcessorViolations = files[1].split('<violation');
            installProcessorViolations.shift();
            // There should be one violation.
            expect(installProcessorViolations.length).to.equal(1, 'Should be one violation detected in InstallProcessorTests.cls');
            expect(installProcessorViolations[0]).to.match(/beginline="953".+rule="ApexUnitTestClassShouldHaveAsserts"/);
          });
      });

      describe('Test Case: Running rules against a glob', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            // NOTE: When running the command for real, a glob would have to be wrapped in SINGLE-QUOTES. But the tests sidestep that, somehow.
            '--target', 'test/code-samples/apex/*Tests.cls',
            '--ruleset', 'ApexUnit',
            '--format', 'xml'
          ])
          .it('Glob is resolved to files, and those files are evaluated', ctx => {
            // We'll split the output by the <file> tag first, so we can get each file that violated rules.
            const files = ctx.stdout.split('<file');
            // The first list item is going to be the header, so we need to pull that off.
            files.shift();
            // Verify that each set of violations corresponds to the expected file.
            expect(files.length).to.equal(2, 'Only two files should have violated the rules');
            expect(files[0]).to.match(/name="\S+\/test\/code-samples\/apex\/AccountServiceTests.cls"/);
            expect(files[1]).to.match(/name="\S+\/test\/code-samples\/apex\/InstallProcessorTests.cls"/);

            // Now, split each file's violations by the <violation> tag so we can inspect individual violations.
            const acctServiceViolations = files[0].split('<violation');
            acctServiceViolations.shift();
            // There should be four violations.
            expect(acctServiceViolations.length).to.equal(4, 'Should be four violations detected in AccountServiceTests.cls');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(acctServiceViolations[0]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[1]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[2]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(acctServiceViolations[3]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);

            const installProcessorViolations = files[1].split('<violation');
            installProcessorViolations.shift();
            // There should be one violation.
            expect(installProcessorViolations.length).to.equal(1, 'Should be one violation detected in InstallProcessorTests.cls');
            expect(installProcessorViolations[0]).to.match(/beginline="953".+rule="ApexUnitTestClassShouldHaveAsserts"/);
          });
      });

      describe('Test Case: Running multiple rulesets at once', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
            '--ruleset', 'ApexUnit,Style',
            '--format', 'xml'
          ])
          .it('Violations from each rule are logged as an XML', ctx => {
            // We'll split the output by the <violation> tag, so we can get individual violations.
            const violations = ctx.stdout.split('<violation');
            // The first list item is going to be the header, so we need to pull that off.
            violations.shift();
            // There should be eleven violations.
            expect(violations.length).to.equal(11, 'Should be eleven violations detected in the file');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(violations[0]).to.match(/beginline="12".+rule="VariableNamingConventions"/);
            expect(violations[1]).to.match(/beginline="13".+rule="VariableNamingConventions"/);
            expect(violations[2]).to.match(/beginline="62".+rule="MethodNamingConventions"/);
            expect(violations[3]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[4]).to.match(/beginline="66".+rule="MethodNamingConventions"/);
            expect(violations[5]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[6]).to.match(/beginline="70".+rule="MethodNamingConventions"/);
            expect(violations[7]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[8]).to.match(/beginline="74".+rule="MethodNamingConventions"/);
            expect(violations[9]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[10]).to.match(/beginline="78".+rule="MethodNamingConventions"/);
          });
      });

      describe('Test Case: Writing XML results to a file', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
            '--ruleset', 'ApexUnit',
            '--outfile', 'testout.xml'
          ])
          .finally(ctx => {
            // Regardless of what happens in the test itself, we need to delete the file we created.
            if (fs.existsSync('testout.xml')) {
              fs.unlinkSync('testout.xml');
            }
          })
          .it('The violations are written to the file as an XML', ctx => {
            // Verify that the file we wanted was actually created.
            expect(fs.existsSync('testout.xml')).to.equal(true, 'The command should have created the expected output file');
            const fileContents = fs.readFileSync('testout.xml').toString();
            // We'll split the output by the <violation> tag, so we can get individual violations.
            const violations = fileContents.split('<violation');
            // The first list item is going to be the header, so we need to pull that off.
            violations.shift();
            // There should be four violations.
            expect(violations.length).to.equal(4, 'Should be four violations detected in the file');
            // We'll check each violation in enough depth to be confident that the expected violations were returned in the
            // expected order.
            expect(violations[0]).to.match(/beginline="66".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[1]).to.match(/beginline="70".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[2]).to.match(/beginline="74".+rule="ApexUnitTestClassShouldHaveAsserts"/);
            expect(violations[3]).to.match(/beginline="78".+rule="ApexUnitTestClassShouldHaveAsserts"/);
          })
      });
    });

    describe('Output Type: CSV', () => {
      runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
          '--ruleset', 'ApexUnit',
          '--format', 'csv'
        ])
        .it('Properly writes CSV to console', ctx => {
          // Split the output by newline characters and throw away the first entry, so we're left with just the rows.
          const rows = ctx.stdout.trim().split('\n');
          rows.shift();

          // There should be four rows.
          expect(rows.length).to.equal(4, 'Should be four violations detected');

          // Split each row by commas, so we'll have each cell.
          const data = rows.map(val => val.split(','));
          // Verify that each row looks approximately right.
          expect(data[0][3]).to.equal('"66"', 'Violation #1 should occur on the expected line');
          expect(data[1][3]).to.equal('"70"', 'Violation #2 should occur on the expected line');
          expect(data[2][3]).to.equal('"74"', 'Violation #3 should occur on the expected line');
          expect(data[3][3]).to.equal('"78"', 'Violation #4 should occur on the expected line');
          expect(data[0][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #1 should be of the expected type');
          expect(data[1][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #2 should be of the expected type');
          expect(data[2][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #3 should be of the expected type');
          expect(data[3][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #4 should be of the expected type');
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
          '--ruleset', 'ApexUnit',
          '--outfile', 'testout.csv'
        ])
        .finally(ctx => {
          // Regardless of what happens in the test itself, we need to delete the file we created.
          if (fs.existsSync('testout.csv')) {
            fs.unlinkSync('testout.csv');
          }
        })
        .it('Properly writes CSV to file', ctx => {
          // Verify that the correct message is displayed to user
          expect(ctx.stdout).to.contain(messages.getMessage('output.writtenToOutFile', ['testout.csv']));
          expect(ctx.stdout).to.not.contain(messages.getMessage('output.noViolationsDetected', []));

          // Verify that the file we wanted was actually created.
          expect(fs.existsSync('testout.csv')).to.equal(true, 'The command should have created the expected output file');
          const fileContents = fs.readFileSync('testout.csv').toString();
          // Split the output by newline characters and throw away the first entry, so we're left with just the rows.
          const rows = fileContents.trim().split('\n');
          rows.shift();

          // There should be four rows.
          expect(rows.length).to.equal(4, 'Should be four violations detected');

          // Split each row by commas, so we'll have each cell.
          const data = rows.map(val => val.split(','));
          // Verify that each row looks approximately right.
          expect(data[0][3]).to.equal('"66"', 'Violation #1 should occur on the expected line');
          expect(data[1][3]).to.equal('"70"', 'Violation #2 should occur on the expected line');
          expect(data[2][3]).to.equal('"74"', 'Violation #3 should occur on the expected line');
          expect(data[3][3]).to.equal('"78"', 'Violation #4 should occur on the expected line');
          expect(data[0][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #1 should be of the expected type');
          expect(data[1][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #2 should be of the expected type');
          expect(data[2][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #3 should be of the expected type');
          expect(data[3][6]).to.equal('"ApexUnitTestClassShouldHaveAsserts"', 'Violation #4 should be of the expected type');
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
          '--ruleset', 'ApexUnit',
          '--format', 'csv'
        ])
        .it('When no violations are detected, a message is logged to the console', ctx => {
          expect(ctx.stdout).to.contain(messages.getMessage('output.noViolationsDetected'));
        });

        runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
          '--ruleset', 'ApexUnit',
          '--outfile', 'testout.csv'
        ])
        .finally(ctx => {
          // Regardless of what happens in the test itself, we need to delete the file we created.
          if (fs.existsSync('testout.csv')) {
            fs.unlinkSync('testout.csv');
          }
        })
        .it('When --oufile is provided and no violations are detected, output file should not be created', ctx => {
          expect(ctx.stdout).to.contain(messages.getMessage('output.noViolationsDetected', []));
          expect(ctx.stdout).to.not.contain(messages.getMessage('output.writtenToOutFile', ['testout.csv']));
          expect(fs.existsSync('testout.csv')).to.be.false;
        });
    });

    describe('Output Type: Table', () => {
      // The table can't be written to a file, so we're just testing the console.
      runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
          '--ruleset', 'ApexUnit',
          '--format', 'table'
        ])
        .it('Properly writes table to the console', ctx => {
          // Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
          // That will leave us with just the rows.
          const rows = ctx.stdout.trim().split('\n');
          rows.shift();
          rows.shift();

          // There should be four rows, and those rows should contain the appropriate data.
          expect(rows.length).to.equal(4, 'Should be four violations detected');
          expect(rows[0]).to.contain("66", 'Violation #1 should occur at expected line');
          expect(rows[1]).to.contain("70", 'Violation #2 should occur at expected line');
          expect(rows[2]).to.contain("74", 'Violation #3 should occur at expected line');
          expect(rows[3]).to.contain("78", 'Violation #4 should occur at expected line');
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run',
          '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
          '--ruleset', 'ApexUnit',
          '--format', 'table'
        ])
        .it('When no violations are detected, a message is logged to the console', ctx => {
          expect(ctx.stdout).to.contain(messages.getMessage('output.noViolationsDetected'));
        });
    });

    describe('Edge Cases', () => {
      describe('Test case: No output specified', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AccountServiceTests.cls'),
            '--ruleset', 'ApexUnit'
          ])
          .it('When no format is specified, we default to a TABLE', ctx => {
            // Split the output by newline characters and throw away the first two rows, which are the column names and a separator.
            // That will leave us with just the rows.
            const rows = ctx.stdout.trim().split('\n');
            rows.shift();
            rows.shift();

            // There should be four rows, and those rows should contain the appropriate data.
            expect(rows.length).to.equal(4, 'Should be four violations detected');
            expect(rows[0]).to.contain("66", 'Violation #1 should occur at expected line');
            expect(rows[1]).to.contain("70", 'Violation #2 should occur at expected line');
            expect(rows[2]).to.contain("74", 'Violation #3 should occur at expected line');
            expect(rows[3]).to.contain("78", 'Violation #4 should occur at expected line');
          });
      });

      describe('Test Case: No rules specified', () => {
        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
            '--format', 'xml'
          ])
          .it('When no rules are explicitly specified, all rules are run', ctx => {
            // We'll split the output by the <violation> tag, so we can get individual violations.
            const violations = ctx.stdout.split('<violation');
            // The first list item is going to be the header, so we need to pull that off.
            violations.shift();
            // There should be 84 violations. We won't individually check all of them, because we'd be here all day. We'll just
            // make sure there's the right number of them.
            expect(violations.length).to.equal(84, 'Should be 84 violations detected in the file');
          });

        runTest
          .stdout()
          .stderr()
          .command(['scanner:run',
            '--target', path.join('test', 'code-samples', 'apex', 'AbstractPriceRuleEvaluatorTests.cls'),
            '--format', 'xml',
            '--verbose'
          ])
          .it('When the --verbose flag is supplied, info about implicitly run rules is logged', ctx => {
            // We'll split the output by the <violation> tag, so we can get individual violations.
            const violations = ctx.stdout.split('<violation');
            // Before the violations are logged, there should be 16 log messages about implicitly included PMD categories.
            const regex = new RegExp(events.info.pmdJarImplicitlyRun.replace(/%s/g, '.*'), 'g');
            expect(violations[0].match(regex) || []).to.have.lengthOf(16, 'Should be 16 PMD-related logs, two for each of the eight categories');
          });
      });
    });

    describe('Error handling', () => {
      runTest
        .stdout()
        .stderr()
        .command(['scanner:run', '--ruleset', 'ApexUnit', '--format', 'xml'])
        .it('Error thrown when no target is specified', ctx => {
          expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${messages.getMessage('validations.mustTargetSomething')}`);
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'NotAValidFileName'])
        .it('Error thrown when output file is malformed', ctx => {
          expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${messages.getMessage('validations.outfileMustBeValid')}`);
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run', '--target', 'path/that/does/not/matter', '--ruleset', 'ApexUnit', '--outfile', 'badtype.pdf'])
        .it('Error thrown when output file is unsupported type', ctx => {
          expect(ctx.stderr).to.contain(`ERROR running scanner:run:  ${messages.getMessage('validations.outfileMustBeSupportedType')}`);
        });

      runTest
        .stdout()
        .stderr()
        .command(['scanner:run', '--target', 'path/that/does/not/matter', '--format', 'csv', '--outfile', 'notcsv.xml'])
        .it('Warning logged when output file format does not match format', ctx => {
          expect(ctx.stdout).to.contain(messages.getMessage('validations.outfileFormatMismatch', ['csv', 'xml']));
        });
    });
  });
});

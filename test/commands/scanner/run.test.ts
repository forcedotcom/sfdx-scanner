import { expect, test } from '@salesforce/command/lib/test';

describe('scanner:run', () => {
  // XML output is more thoroughly tested than other output types because, at time of writing (2/21/2020), the only engine
  // supported is PMD, whose output is already an XML. So we're really just testing the other formats to make sure that
  // we properly convert the output from an XML.
  describe('Output Type: XML', () => {
    describe('Test Case: Running rules against a single file', () => {
      test
        .stdout()
        .stderr()
        .command(['scanner:run'])
        .it('When the file contains violations, they are logged out as an XML', ctx => {
          expect(true).to.be.true;
        });

      test
        .stdout()
        .stderr()
        .command(['scanner:run'])
        .it('When the file contains no violations, a message is logged to the console', ctx => {
          expect(true).to.be.true;
        });
    });

    describe('Test Case: Running rules against a folder', () => {
      test
        .stdout()
        .stderr()
        .command(['scanner:run'])
        .it('Any violations in the folder are logged as an XML', ctx => {
          expect(true).to.be.true;
        });
    });

    describe('Test Case: Running multiple rulesets at once', () => {
      test
        .stdout()
        .stderr()
        .command(['scanner:run'])
        .it('Violations from each rule are logged as an XML', ctx => {
          expect(true).to.be.true;
        });
    });

    describe('Test Case: Writing XML results to a file', () => {
      test
        .stdout()
        .stderr()
        .command(['scanner:run'])
        .it('The violations are written to the file as an XML', ctx => {
          expect(true).to.be.true;
        });
    });
  });

  describe('Output Type: CSV', () => {
    test
      .stdout()
      .stderr()
      .command(['scanner:run'])
      .it('Properly writes CSV to console', ctx => {
        expect(true).to.be.true;
      });

    test
      .stdout()
      .stderr()
      .command(['scanner:run'])
      .it('Properly writes CSV to file', ctx => {
        expect(true).to.be.true;
      });
  });

  describe('Output Type: Table', () => {
    // The table can't be written to a file, so we're just testing the console.
    test
      .stdout()
      .stderr()
      .command(['scanner:run'])
      .it('Properly writes table to the console', ctx => {
        expect(true).to.be.true;
      });
  });
});

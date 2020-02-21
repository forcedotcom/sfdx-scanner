import { expect, test } from '@salesforce/command/lib/test';

describe('scanner:run', () => {
  // XML output is more thoroughly tested than other output types because, at time of writing (2/21/2020), the only engine
  // supported is PMD, whose output is already an XML. So we're really just testing the other formats to make sure that
  // we properly convert the output from an XML.
  describe('Output Type: XML', () => {
    describe('Test Case: Running rules against a single file', () => {

    });

    describe('Test Case: Running rules against a folder', () => {

    });

    describe('Test Case: Running multiple rulesets at once', () => {

    });

    describe('Test Case: Writing XML results to a file', () => {

    });
  });

  describe('Output Type: CSV', () => {
    describe('Test Case: Writing results to the console', () => {

    });

    describe('Test Case: Writing results to a file', () => {

    });
  });

  describe('Output Type: Table', () => {
    // The table can't be written to a file, so we're just testing the console.
    describe('Test Case: Writing results to the console', () => {

    });
  })
});

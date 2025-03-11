import fs from 'node:fs';
import { OutputFormat } from '@salesforce/code-analyzer-core';
import { RulesFileWriter } from "../../../src/lib/writers/RulesWriter";
import * as Stub from '../../stubs/StubRuleSelection';

describe('RulesWriter', () => {

    let writeFileSpy: jest.SpyInstance;
    let writeFileInvocations: { file: fs.PathOrFileDescriptor, contents: string | ArrayBufferView }[];

    beforeEach(() => {
        writeFileInvocations = [];
        writeFileSpy = jest.spyOn(fs, 'writeFileSync').mockImplementation((file, contents) => {
            writeFileInvocations.push({file, contents});
        });
    });

    it('Rejects invalid file format', () => {
        const invalidFile = 'file.xml';
        expect(() => new RulesFileWriter(invalidFile)).toThrow(invalidFile);
    });

    it('Writes to a file in JSON format', () => {
        const expectations = {
            file: 'rules.json',
            contents: `Rules formatted as ${OutputFormat.JSON}`
        };
        const rulesWriter = new RulesFileWriter(expectations.file);
        const stubbedEmptyRuleSelection = new Stub.StubEmptyRuleSelection();
        rulesWriter.write(stubbedEmptyRuleSelection);

        expect(writeFileSpy).toHaveBeenCalled();
		expect(writeFileInvocations).toEqual([expectations]);
    });

    it('Writes to a windows file path', () => {
        const windowsPath = 'C:\\Users\\test\\file.json';
        const expectations = {
            file: windowsPath,
            contents: `Rules formatted as ${OutputFormat.JSON}`
        };
        const rulesWriter = new RulesFileWriter(expectations.file);
        const stubbedEmptyRuleSelection = new Stub.StubEmptyRuleSelection();
        rulesWriter.write(stubbedEmptyRuleSelection);

        expect(writeFileSpy).toHaveBeenCalled();
		expect(writeFileInvocations).toEqual([expectations]);
    });

    it('Writes to a linux file path', () => {
        const linuxPath = '/Users/test/file.json';
        const expectations = {
            file: linuxPath,
            contents: `Rules formatted as ${OutputFormat.JSON}`
        };
        const rulesWriter = new RulesFileWriter(expectations.file);
        const stubbedEmptyRuleSelection = new Stub.StubEmptyRuleSelection();
        rulesWriter.write(stubbedEmptyRuleSelection);

        expect(writeFileSpy).toHaveBeenCalled();
		expect(writeFileInvocations).toEqual([expectations]);
    });
});
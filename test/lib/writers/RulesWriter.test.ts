import { OutputFormat } from '@salesforce/code-analyzer-core';
import fs from 'node:fs';
import path from 'node:path';
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

    it('Writes to a json file path', () => {
        const outfilePath = path.join('the', 'results', 'path', 'file.json');
        const expectations = {
            file: outfilePath,
            contents: `Rules formatted as ${OutputFormat.JSON}`
        };
        const rulesWriter = new RulesFileWriter(expectations.file);
        const stubbedEmptyRuleSelection = new Stub.StubEmptyRuleSelection();
        rulesWriter.write(stubbedEmptyRuleSelection);

        expect(writeFileSpy).toHaveBeenCalled();
		expect(writeFileInvocations).toEqual([expectations]);
    });
});
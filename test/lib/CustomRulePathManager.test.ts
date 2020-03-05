import { CustomRulePathManager, ENGINE } from '../../src/lib/CustomRulePathManager';
import { FileIOHandler } from '../../src/lib/FileIOHandler';
import { expect } from 'chai';

import Sinon = require('sinon');
// import { Messages } from '@salesforce/core';

// Messages.importMessagesDirectory(__dirname);
// const messages = Messages.loadMessages('scanner', 'add');

// test cases:
// read entries on initialization
// don't run initialization if it was already run

describe('CustomRulePathManager tests', () => {

    let sandbox: Sinon.SinonSandbox;

    before(() => { sandbox = Sinon.createSandbox(); });
    afterEach(() => { sandbox.restore(); });

    // File exists, but content has been somehow cleared
    const emptyFile = '';

    // One entry for apex, two for java
    const populatedFile = '{"pmd": {"apex": ["/some/user/path/customRule.jar"],"java": ["/abc/def/ghi","/home/lib/jars"]}}';

    describe('Rule path entries creation', () => {
        it('should read CustomPaths.json to get Rule Path Entries', async () => {
            // Setup stub
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(populatedFile);
            const manager = new CustomRulePathManager(stub);

            // Execute test
            const rulePathMap = await manager.getRulePathEntries(ENGINE.PMD);
            rulePathMap.forEach((value, key) => {
                console.log(`${key}: ${Array.from(value)}`);
            });

            // Validate run
            expect(stub.readFile.calledOnce).to.be.true;
            expect(rulePathMap).to.be.lengthOf(2);

            //Validate each entry
            expect(rulePathMap).has.keys('apex', 'java');
            const apexPaths = rulePathMap.get('apex');
            expect(apexPaths).to.be.lengthOf(1);
            expect(apexPaths).deep.contains('/some/user/path/customRule.jar');

            const javaPaths = rulePathMap.get('java');
            expect(javaPaths).to.be.lengthOf(2);
            expect(javaPaths).deep.contains('/abc/def/ghi');
            expect(javaPaths).deep.contains('/home/lib/jars');
        });

        it('should handle empty Rule Path file gracefully', async () => {
            // Setup stub
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(emptyFile);
            const manager = new CustomRulePathManager(stub);

            // Execute test
            const rulePathMap = await manager.getRulePathEntries(ENGINE.PMD);

            // Validate
            expect(rulePathMap).is.not.null;
            expect(rulePathMap).to.be.lengthOf(0);

        });

        // it('should handle non-existent CustomPath.json file gracefully', async () => {
        //     // Setup stub
        //     let stub = Sinon.createStubInstance(FileOperations);
        //     const dummyError = new Error('Test error to indicate file does not exist');

        //     // TODO: how do I set code as ENOENT?
        //     stub.readRulePathFile.throws(dummyError);
        //     const manager = new CustomRulePathManager(stub);

        //     // Execute test and validate

        //     await manager.getRulePathEntries(ENGINE.PMD);
        //     expect(await manager.getRulePathEntries.bind(ENGINE.PMD)).throws(messages.getMessage('errors.readCustomRulePathFileFailed'));

        //     // Validate


        // });

        it('should initialize only once', async () => {
            // Setup stub
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(populatedFile);
            const manager = new CustomRulePathManager(stub);

            // Execute test
            await manager.getRulePathEntries(ENGINE.PMD);
            // Rerun same end point again. This time, it shouldn't have read file
            await manager.getRulePathEntries(ENGINE.PMD);

            // Validate
            expect(stub.readFile.calledOnce).to.be.true;
        });
    });

    describe('Adding new Rule Path entries', () => {
        it('should read current entries before appending new entries', async () => {
            // Setup stub
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(populatedFile);
            stub.writeFile.resolves(); // do nothing
            stub.mkdirIfNotExists.resolves(); // do nothing
            const manager = new CustomRulePathManager(stub);

            // Execute test
            await manager.addPathsForLanguage('language', ['path1', 'path2']);

            // Validate
            expect(stub.readFile.calledOnce).to.be.true;
            expect(stub.mkdirIfNotExists.calledOnce).to.be.true;
            expect(stub.writeFile.calledOnce).to.be.true;

        });

        it('should reflect newly added entries', async () => {
            // Setup stub
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(emptyFile);
            stub.writeFile.resolves(); // do nothing
            stub.mkdirIfNotExists.resolves(); // do nothing
            const manager = new CustomRulePathManager(stub);
            const language = 'javascript';
            const path = ['path1', 'path2'];

            // Execute test - fetch original state of Map, add entries, check state of updated Map
            const originalRulePathMap = await manager.getRulePathEntries(ENGINE.PMD);
            await manager.addPathsForLanguage(language, path);
            const updatedRulePathMap = await manager.getRulePathEntries(ENGINE.PMD);

            // Validate
            expect(originalRulePathMap).to.be.empty;
            expect(updatedRulePathMap).to.have.keys(language);
            const pathFromRun = updatedRulePathMap.get(language);
            expect(pathFromRun).to.contain(path[0]);
            expect(pathFromRun).to.contain(path[1]);
        });

        it('should append path entries if language already exists', async () => {
            // Setup stub
            const fileContent = '{"pmd": {"apex": ["/some/user/path/customRule.jar"]}}';
            let stub = Sinon.createStubInstance(FileIOHandler);
            stub.readFile.resolves(fileContent);
            stub.writeFile.resolves(); // do nothing
            const manager = new CustomRulePathManager(stub);
            const language = 'apex';
            const newPath = '/my/new/path';

            // Execute test
            const originalRulePathMap = await manager.getRulePathEntries(ENGINE.PMD);
            // Pre-validate to make sure test setup is alright, before proceeding
            expect(originalRulePathMap).has.keys([language]);
            const orginalPathEntries = originalRulePathMap.get(language);
            expect(orginalPathEntries).to.be.lengthOf(1);
            // Now add more entries to same language
            await manager.addPathsForLanguage(language, [newPath]);
            // Fetch updated Map to validate
            const updatedRulePathMap = await manager.getRulePathEntries(ENGINE.PMD);

            // Validate
            const newPathEntries = updatedRulePathMap.get(language);
            expect(newPathEntries).to.be.lengthOf(2);
            expect(newPathEntries).to.contain(newPath);
        });
    });
});

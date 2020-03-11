import {expect} from 'chai';
import {Stats} from 'fs';
import {CustomRulePathManager, ENGINE} from '../../src/lib/CustomRulePathManager';
import {FileHandler} from '../../src/lib/FileHandler';
import Sinon = require('sinon');

/**
 * Unit tests to verify CustomRulePathManager
 * TODO: Add tests to cover exception scenarios when CustomPath.json does not exist
 */

describe('CustomRulePathManager tests', () => {

    // File exists, but content has been somehow cleared
    const emptyFile = '';

    // One entry for apex, two for java
    const populatedFile = '{"pmd": {"apex": ["/some/user/path/customRule.jar"],"java": ["/abc/def/ghi","/home/lib/jars"]}}';

    describe('Rule path entries creation', () => {

        describe('with pre-populated file', () => {
            let readStub;
            beforeEach(() => {
                readStub = Sinon.stub(FileHandler.prototype, 'readFile').resolves(populatedFile);
            });

            afterEach(() => {
                readStub.restore();
            });

            it('should read CustomPaths.json to get Rule Path Entries', async () => {
                const manager = new CustomRulePathManager();

                // Execute test
                const rulePathMap = await manager.getRulePathEntries(ENGINE.PMD);

                // Validate run
                expect(readStub.calledOnce).to.be.true;
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

            it('should initialize only once', async () => {
                const manager = new CustomRulePathManager();

                // Execute test
                await manager.getRulePathEntries(ENGINE.PMD);
                // Rerun same end point again. This time, it shouldn't have read file
                await manager.getRulePathEntries(ENGINE.PMD);

                // Validate
                expect(readStub.calledOnce).to.be.true;
            });
        });


        it('should handle empty Rule Path file gracefully', async () => {
            // Setup stub
            const readStub = Sinon.stub(FileHandler.prototype, 'readFile').resolves(emptyFile);
            const manager = new CustomRulePathManager();

            // Execute test
            const rulePathMap = await manager.getRulePathEntries(ENGINE.PMD);

            // Validate
            expect(readStub.calledOnce).to.be.true;
            expect(rulePathMap).is.not.null;
            expect(rulePathMap).to.be.lengthOf(0);

            readStub.restore();
        });
    });

    describe('Adding new Rule Path entries', () => {

        let statsStub, readDirStub, readStub, writeStub, mkdirStub;
        before(() => {
            writeStub = Sinon.stub(FileHandler.prototype, 'writeFile').resolves();
            mkdirStub = Sinon.stub(FileHandler.prototype, 'mkdirIfNotExists').resolves();

            const mockDirStats = new Stats();
            mockDirStats.isDirectory = () => true;
            mockDirStats.isFile = () => false;
            statsStub = Sinon.stub(FileHandler.prototype, 'stats').resolves(mockDirStats);
            readDirStub = Sinon.stub(FileHandler.prototype, 'readDir').resolves([]);
        });

        afterEach(() => {
            readStub.restore();
        });

        after(() => {
            Sinon.restore();
        });


        it('should read current entries before appending new entries', async () => {
            readStub = Sinon.stub(FileHandler.prototype, 'readFile').resolves(populatedFile);
            const manager = new CustomRulePathManager();

            // Execute test
            await manager.addPathsForLanguage('language', ['path1', 'path2']);

            // Validate
            expect(statsStub.calledTwice).to.be.true; // Once per path
            expect(readDirStub.calledTwice).to.be.true; // Once per path
            expect(readStub.calledOnce).to.be.true;
            expect(mkdirStub.calledOnce).to.be.true;
            expect(writeStub.calledOnce).to.be.true;

        });

        it('should reflect newly added entries', async () => {
            readStub = Sinon.stub(FileHandler.prototype, 'readFile').resolves(emptyFile);
            const manager = new CustomRulePathManager();
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
            readStub = Sinon.stub(FileHandler.prototype, 'readFile').resolves(fileContent);

            const manager = new CustomRulePathManager();
            const language = 'apex';
            const newPath = '/my/new/path';

            // Execute test
            const originalRulePathMap = await manager.getRulePathEntries(ENGINE.PMD);
            // Pre-validate to make sure test setup is alright, before proceeding
            expect(originalRulePathMap).has.keys([language]);
            const originalPathEntries = originalRulePathMap.get(language);
            expect(originalPathEntries).to.be.lengthOf(1);
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

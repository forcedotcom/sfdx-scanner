import {expect} from 'chai';
import {FileHandler} from '../../src/lib/util/FileHandler';
import {Config} from '../../src/lib/util/Config';
import Sinon = require('sinon');
import {verifyJreSetup, JreSetupManagerDependencies} from '../../src/lib/JreSetupManager';
import childProcess = require('child_process');
import { Messages } from '@salesforce/core';
import { before } from 'mocha';
// Initialize Messages with the current plugin directory
Messages.importMessagesDirectory(__dirname);

describe('JreSetupManager #verifyJreSetup', () => {
    const javaHomeValidPath = '/valid/java/home';
    const javaHomeInvalidPath = '/invalid/java/home';
    const noError = undefined;
    const error = new Error('Dummy error from test');
    const emptyStdout = '';
    const validVersion11 = 'openjdk version "11.0.6" 2020-01-14 LTS\nOpenJDK Runtime Environment Zulu11.37+17-CA (build 11.0.6+10-LTS)\nOpenJDK 64-Bit Server VM Zulu11.37+17-CA (build 11.0.6+10-LTS, mixed mode)\n';
    const validVersion8 = 'openjdk version "1.8.0_172"\nOpenJDK Runtime Environment (Zulu 8.30.0.2-macosx) (build 1.8.0_172-b01)\nOpenJDK 64-Bit Server VM (Zulu 8.30.0.2-macosx) (build 25.172-b01, mixed mode)\n';
    const validVersion14Win = 'openjdk 14 2020-03-17\r\nOpenJDK Runtime Environment (build 14+36-1461)\r\nOpenJDK 64-Bit Server VM (build 14+36-1461, mixed mode, sharing)\r\n';
    const invalidVersion = 'openjdk version "1.5.0_188"\nOpenJDK Runtime Environment';

    describe('With valid javaHome path in Config and an accepted Java version', () => {

        let setJavaHomeStub;
        before(() => {
            Sinon.createSandbox();
            // Config file exists and has the valid path
            Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
            setJavaHomeStub = Sinon.stub(Config.prototype, 'setJavaHome').resolves();
            
            // FileHandler stat confirms that path is valid
            Sinon.stub(FileHandler.prototype, 'stats').resolves();

            // java command brings back a valid and acceptable java version
            Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion8);
        });

        after(() => {
            Sinon.restore();
        });

        it('should set correct Key in config', async () => {
            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            const javaHomeValue = setJavaHomeStub.getCall(0).args[0];
            expect(javaHomeValue).equals(javaHomeValidPath);
            expect(setJavaHomeStub.calledOnce).to.be.true;
            expect(javaHome).equals(javaHomeValidPath);
        });

    });

    describe('With no Config entry, but valid path in System variable', () => {
        const env = process.env;

        before(() => {
            Sinon.createSandbox();
            // Config file exists and has the valid path
            Sinon.stub(Config.prototype, 'getJavaHome').returns('');

            // FileHandler stat confirms that path is valid
            Sinon.stub(FileHandler.prototype, 'stats').resolves();

            // java command brings back a valid and acceptable java version
            Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion8);

            // Stub the interactions with Config file
            Sinon.stub(Config.prototype, 'setJavaHome').resolves();
        });

        after(() => {
            Sinon.restore();
            process.env = env;
        });

        it('should check JAVA_HOME for path', async () => {
            process.env = { JAVA_HOME: javaHomeValidPath};

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(javaHome).equals(javaHomeValidPath);
        });

        it('should check JRE_HOME for path', async () => {
            process.env = { JRE_HOME: javaHomeValidPath};

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(javaHome).equals(javaHomeValidPath);
        });

        it('should check JDK_HOME for path', async () => {
            process.env = { JDK_HOME: javaHomeValidPath};

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(javaHome).equals(javaHomeValidPath);
        });
    });

    describe('With no Config entry or System variable, but can auto detect a valid javaHome', () => {
        const env = process.env;
        before(() => {
            Sinon.createSandbox();
            // Config file exists and has the valid path
            Sinon.stub(Config.prototype, 'getJavaHome').returns('');

            // No System variables in process.env
            process.env = {};

            // FileHandler stat confirms that path is valid
            Sinon.stub(FileHandler.prototype, 'stats').resolves();

            // java command brings back a valid and acceptable java version
            Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion8);

            // Stub the interactions with Config file
            Sinon.stub(Config.prototype, 'setJavaHome').resolves();
        });

        after(() => {
            Sinon.restore();
            process.env = env;
        });

        it('should handle successful javaHome auto detection', async () => {
            const findJavaHomeStub = Sinon.stub(JreSetupManagerDependencies.prototype, 'autoDetectJavaHome').resolves(javaHomeValidPath);

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(findJavaHomeStub.calledOnce).to.be.true;
            expect(javaHome).equals(javaHomeValidPath);

            findJavaHomeStub.restore();
            
        });

        it('should handle failed javaHome auto detection', async () => {
            const findJavaHomeStub = Sinon.stub(JreSetupManagerDependencies.prototype, 'autoDetectJavaHome').resolves(null);

            // Execute and verify
            try {
                await verifyJreSetup();
            } catch (err) {
                expect(err.name).equals('NoJavaHomeFound');
            }

            expect(findJavaHomeStub.calledOnce).to.be.true;
            
            findJavaHomeStub.restore();
            
        });
    });

    describe('With Config entry leading to different outcomes', () => {

        before(() => {
            Sinon.createSandbox();         
            // Stub the interactions with Config file
            Sinon.stub(Config.prototype, 'setJavaHome').resolves();
        });

        after(() => {
            Sinon.restore();
        });

        it('should fail when invalid path is found', async () => {
            // More stubbing
            const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeInvalidPath);
            // FileHandler stat claims that path is invalid
            const statStub = Sinon.stub(FileHandler.prototype, 'stats').throws(error);

            // Execute and verify
            try {
                await verifyJreSetup();
            } catch (err) {
                expect(err.name).equals('InvalidJavaHome');
            }

            configGetJavaHomeStub.restore();
            statStub.restore();
        });

        it('should fail when valid path is found, but Java version is not acceptable', async () => {
            // More stubbing
            const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
            const statStub = Sinon.stub(FileHandler.prototype, 'stats').resolves();
            // Invalid java version is returned
            const execStub = Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, invalidVersion);

            // Execute and verify
            try {
                await verifyJreSetup();
            } catch (err) {
                expect(err.name).equals('InvalidVersion');
            }

            configGetJavaHomeStub.restore();
            statStub.restore();
            execStub.restore();
        });

        it('should finish successfully when Java11 is found', async () => {
            // More stubbing
            const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
            const statStub = Sinon.stub(FileHandler.prototype, 'stats').resolves();
            const execStub = Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion11);

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(javaHome).equals(javaHomeValidPath);

            configGetJavaHomeStub.restore();
            statStub.restore();
            execStub.restore();
        });

        it('should finish successfully when Java14 is found on Windows', async () => {
            // More stubbing
            const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
            const statStub = Sinon.stub(FileHandler.prototype, 'stats').resolves();
            const execStub = Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion14Win);

            // Execute
            const javaHome = await verifyJreSetup();

            // Verify
            expect(javaHome).equals(javaHomeValidPath);

            configGetJavaHomeStub.restore();
            statStub.restore();
            execStub.restore();
        });

    });
});
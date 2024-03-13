import {expect} from 'chai';
import {FileHandler} from '../../src/lib/util/FileHandler';
import {Config} from '../../src/lib/util/Config';
import Sinon = require('sinon');
import {verifyJreSetup, JreSetupManagerDependencies} from '../../src/lib/JreSetupManager';
import childProcess = require('child_process');
import * as TestOverrides from '../test-related-lib/TestOverrides';

TestOverrides.initializeTestSetup();

describe('JreSetupManager #verifyJreSetup', () => {
	const javaHomeValidPath = '/valid/java/home';
	const javaHomeInvalidPath = '/invalid/java/home';
	const noError = undefined;
	const error = new Error('Dummy error from test');
	const emptyStdout = '';
	const validVersion11 = 'openjdk version "11.0.6" 2020-01-14 LTS\nOpenJDK Runtime Environment Zulu11.37+17-CA (build 11.0.6+10-LTS)\nOpenJDK 64-Bit Server VM Zulu11.37+17-CA (build 11.0.6+10-LTS, mixed mode)\n';
	const invalidVersion = 'openjdk version "1.8.0_172"\nOpenJDK Runtime Environment (Zulu 8.30.0.2-macosx) (build 1.8.0_172-b01)\nOpenJDK 64-Bit Server VM (Zulu 8.30.0.2-macosx) (build 25.172-b01, mixed mode)\n';
	const validVersion12 = 'java version "12.0.1" 2019-04-16\nJava(TM) SE Runtime Environment (build 12.0.1+12)\nJava HotSpot(TM) 64-Bit Server VM (build 12.0.1+12, mixed mode, sharing)';
	const validVersion14Win = 'openjdk 14 2020-03-17\r\nOpenJDK Runtime Environment (build 14+36-1461)\r\nOpenJDK 64-Bit Server VM (build 14+36-1461, mixed mode, sharing)\r\n';

	describe('With valid javaHome path in Config and an accepted Java version', () => {

		let setJavaHomeStub;
		beforeEach(() => {
			Sinon.createSandbox();
			// Config file exists and has the valid path
			Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
			setJavaHomeStub = Sinon.stub(Config.prototype, 'setJavaHome').resolves();

			// FileHandler stat confirms that path is valid
			Sinon.stub(FileHandler.prototype, 'stats').resolves();

			// java command brings back a valid and acceptable java version
			Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion11);
		});

		afterEach(() => {
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

		beforeEach(() => {
			Sinon.createSandbox();
			// Config file exists and has the valid path
			Sinon.stub(Config.prototype, 'getJavaHome').returns('');

			// FileHandler stat confirms that path is valid
			Sinon.stub(FileHandler.prototype, 'stats').resolves();

			// java command brings back a valid and acceptable java version
			Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion11);

			// Stub the interactions with Config file
			Sinon.stub(Config.prototype, 'setJavaHome').resolves();
		});

		afterEach(() => {
			Sinon.restore();
			process.env = env;
		});

		it('should check JAVA_HOME for path', async () => {
			process.env = {JAVA_HOME: javaHomeValidPath};

			// Execute
			const javaHome = await verifyJreSetup();

			// Verify
			expect(javaHome).equals(javaHomeValidPath);
		});

		it('should check JRE_HOME for path', async () => {
			process.env = {JRE_HOME: javaHomeValidPath};

			// Execute
			const javaHome = await verifyJreSetup();

			// Verify
			expect(javaHome).equals(javaHomeValidPath);
		});

		it('should check JDK_HOME for path', async () => {
			process.env = {JDK_HOME: javaHomeValidPath};

			// Execute
			const javaHome = await verifyJreSetup();

			// Verify
			expect(javaHome).equals(javaHomeValidPath);
		});
	});

	describe('With no Config entry or System variable, but can auto detect a valid javaHome', () => {
		const env = process.env;
		beforeEach(() => {
			Sinon.createSandbox();
			// Config file exists and has the valid path
			Sinon.stub(Config.prototype, 'getJavaHome').returns('');

			// No System variables in process.env
			process.env = {};

			// FileHandler stat confirms that path is valid
			Sinon.stub(FileHandler.prototype, 'stats').resolves();

			// java command brings back a valid and acceptable java version
			Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion11);

			// Stub the interactions with Config file
			Sinon.stub(Config.prototype, 'setJavaHome').resolves();
		});

		afterEach(() => {
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

		beforeEach(() => {
			Sinon.createSandbox();
			// Stub the interactions with Config file
			Sinon.stub(Config.prototype, 'setJavaHome').resolves();
		});

		afterEach(() => {
			Sinon.restore();
		});

		it('should fail when invalid path is found', async () => {
			// More stubbing
			const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeInvalidPath);
			// FileHandler stat claims that path is invalid
			const statStub = Sinon.stub(FileHandler.prototype, 'stats').throws(error);

			// Execute and verify
			let errorThrown: boolean;
			let errName: string;
			try {
				await verifyJreSetup();
				errorThrown = false;
			} catch (err) {
				errorThrown = true;
				errName = err.name;
			}
			expect(errorThrown).to.equal(true, 'Should have failed');
			expect(errName).equals('InvalidJavaHome');


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
				expect.fail('Should have thrown an exception');
			} catch (err) {
				expect(err.name).equals('InvalidVersion');
				expect(err.message).contains('1.8');
			}


			configGetJavaHomeStub.restore();
			statStub.restore();
			execStub.restore();
		});

		it('should finish successfully when Java12 is found', async () => {
			// More stubbing
			const configGetJavaHomeStub = Sinon.stub(Config.prototype, 'getJavaHome').returns(javaHomeValidPath);
			const statStub = Sinon.stub(FileHandler.prototype, 'stats').resolves();
			const execStub = Sinon.stub(childProcess, 'execFile').yields(noError, emptyStdout, validVersion12);

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

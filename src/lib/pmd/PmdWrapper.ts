import {Logger} from '@salesforce/core';
import {PMD_LIB} from '../../Constants';
import {PmdSupport, PmdSupportOptions} from './PmdSupport';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');
import {FileHandler} from '../util/FileHandler';

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

type PmdWrapperOptions = PmdSupportOptions & {
	targets: string[];
	rules: string;
};

export default class PmdWrapper extends PmdSupport {
	private targets: string[];
	private rules: string;
	private logger: Logger;
	private initialized: boolean;

	public constructor(opts: PmdWrapperOptions) {
		super(opts);
		this.targets = opts.targets;
		this.rules = opts.rules;
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('PmdWrapper');
		this.initialized = true;
	}

	public async execute(): Promise<string> {
		return super.runCommand();
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// The classpath needs PMD's lib folder. There may be redundancy with the shared classpath, but having the
		// same JAR in the classpath twice is fine. Also note that the classpath is not wrapped in quotes like how it
		// would be if we invoked directly through the CLI, because child_process.spawn() hates that.
		const classpath = [`${PMD_LIB}/*`, ...this.buildSharedClasspath()].join(path.delimiter);
		// Operating systems impose limits on the maximum length of a command line invocation. This can be problematic
		// when scanning a large number of files. Store the list of files to scan in a temp file. Pass the location
		// of the temp file to PMD. The temp file is cleaned up when the process exits.
		const fileHandler = new FileHandler();
		const tmpPath = await fileHandler.tmpFileWithCleanup();
		await fileHandler.writeFile(tmpPath, this.targets.join(','));
		const args = ['-cp', classpath, HEAP_SIZE, MAIN_CLASS, '-filelist', tmpPath,
			'-format', 'xml'];
		if (this.rules.length > 0) {
			args.push('-rulesets', this.rules);
		}

		this.logger.trace(`Preparing to execute PMD with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// PMD's convention is that an exit code of 0 indicates a successful run with no violations, and an exit code of
		// 4 indicates a successful run with at least one violation.
		return code === 0 || code === 4;
	}
}

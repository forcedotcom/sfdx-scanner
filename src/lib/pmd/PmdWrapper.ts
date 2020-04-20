import {ChildProcessWithoutNullStreams} from 'child_process';
import {Logger, Messages} from '@salesforce/core';
import {Format, PmdSupport} from './PmdSupport';
import * as JreSetupManager from './../JreSetupManager';
import {uxEvents} from '../ScannerEvents';
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

interface PmdWrapperOptions {
	path: string;
	rules: string;
	reportFormat?: Format;
	reportFile?: string;
}

export default class PmdWrapper extends PmdSupport {


	path: string;
	rules: string;
	reportFormat: Format;
	reportFile: string;
	logger: Logger; // TODO: Add relevant trace log lines
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		this.logger = await Logger.child('PmdWrapper');

		this.initialized = true;
	}

	public static async execute(path: string, rules: string, reportFormat?: Format, reportFile?: string): Promise<[boolean, string]> {
		const myPmd = await PmdWrapper.create({
			path: path,
			rules: rules,
			reportFormat: reportFormat,
			reportFile: reportFile
		});
		return myPmd.execute();
	}

	private async execute(): Promise<[boolean, string]> {
		return super.runCommand();
	}

	constructor(options: PmdWrapperOptions) {
		super(options);
		this.path = options.path;
		this.rules = options.rules;
		this.reportFormat = options.reportFormat || Format.XML;
		this.reportFile = options.reportFile || null;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// Start with the arguments we know we'll always need.
		// NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
		// is intended for child_process.spawn(), which freaks out if you do that.
		const classpath = await super.buildClasspath();
		let args = ['-cp', classpath.join(path.delimiter), HEAP_SIZE, MAIN_CLASS, '-rulesets', this.rules, '-dir', this.path,
			'-format', this.reportFormat];

		// Then add anything else that's dynamically included based on other input.
		if (this.reportFile) {
			args = [...args, '-reportfile', this.reportFile];
		}

		this.logger.trace(`Preparing to execute PMD with command: "${command}", args: "${args}"`);
		return [command, args];
	}

	/**
	 * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
	 * Resolves/rejects the Promise once the child process finishes.
	 * @param cp
	 * @param res
	 * @param rej
	 */
	protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: ([boolean, string]) => void, rej: (string) => void): void {
		let stdout = '';
		let stderr = '';

		// When data is passed back up to us, pop it onto the appropriate string.
		cp.stdout.on('data', data => {
			stdout += data;
		});
		cp.stderr.on('data', data => {
			stderr += data;
		});

		cp.on('exit', code => {
			this.logger.trace(`monitorChildProcess has received exit code ${code}`);
			if (code === 0 || code === 4) {
				const processedStdout = this.turnErrorsIntoWarnings(stdout);
				// If the exit code is 0, then no rule violations were found. If the exit code is 4, then it means that at least
				// one violation was found. In either case, PMD ran successfully, so we'll resolve the Promise. We use a tuple
				// containing a boolean that will be true if there were any violations, and stdut.
				// That way, we have a simple indicator of whether there were violations, and a log that we can sweep to know
				// what those violations were.
				res([!!code, processedStdout]);
			} else {
				// If we got any other error, it means something actually went wrong. We'll just reject with stderr for the ease
				// of upstream error handling.
				rej(stderr);
			}
		});
	}

	/**
	 * Sweeps the output from PMD, converting any errors thrown while inspecting files into warnings that can be surfaced
	 * to the user. Said errors are also removed from the output so the remainder can be properly parsed downstream.
	 * @param stdout - The stdout from a successful run of of PMD. Reminder that a successful run is one that exited with
	 *                 status code of 0 or 4.
	 * @return - The input string, but with any errors removed.
	 */
	protected turnErrorsIntoWarnings(stdout: string): string {
		// This method is currently only capable of dealing with XML output. That's fine currently, because we've pretty much
		// hardcoded PMD to output XMLs. But if that changes, then this method will need to be updated too.
		if (this.reportFormat !== Format.XML) {
			return stdout;
		}

		// Any thrown errors should be in an <error> tag. So we'll split the output, using the following regex. It will catch
		// the opening '<error' or the closing '</error>', and pull off any whitespace at the end. That way, we'll instantly
		// be able to tell what we're looking at when we examine each piece.
		const splitStdout = stdout.split(/<error\s*|<\/error>\s*/g);
		if (splitStdout.length === 1) {
			// If there's only one entry, then it means there weren't any errors, so we can just return the string.
			return stdout;
		} else {
			// If there's more than one entry, then we've got some errors we need to process. We'll gradually build up our processed
			// string.
			const outputArray = [];
			splitStdout.forEach(piece => {
				if (piece === '') {
					// If the piece is an empty string, that means there were two separators next to each other. Skip it.
					return;
				} else if (piece.startsWith('<')) {
					// If the piece starts with a bracket, it means that this is NOT the contents of an error tag. So just trim the
					// whitespace since we'll add our own at the end, and push it onto our array.
					outputArray.push(piece.trim());
				} else if (piece.startsWith('filename')) {
					// If the piece starts with 'filename', then it's the contents of an error tag. We know that because filename is
					// the first argument provided to the tag.
					// We'll use the contents of the tag to construct a warning that will be surfaced to the user.
					// We'll need the filename and the failure message. They'll be between the first and second '"', and between the
					// third and fourth '"' respectively.
					const filename = piece.split('"')[1];
					const msg = piece.split('"')[3];
					uxEvents.emit('warning-always', messages.getMessage('warning.pmdSkippedFile', [filename, msg]));
				}
			});
			// Once we've processed all the pieces, we can join what we've got and return it.
			return outputArray.join('\n');
		}
	}
}

import core = require("@actions/core");
import {ClassDescriptor} from './types';
import {summarizeErrors as summarizeJUnitErrors} from './summarizeJUnitErrors';
import {summarizeErrors as summarizeMochaErrors} from './summarizeMochaErrors';

const UNKNOWN_FAILURE_MESSAGE = `Something failed with this project, but the action can't tell what. Consult the artifact and check manually.
Make sure you check code coverage!`;

const SUMMARIZER_BY_FRAMEWORK = new Map([
	['junit', summarizeJUnitErrors],
	['mocha', summarizeMochaErrors]
]);

/**
 * Adds a GHA summary including details about tests that failed.
 */
async function run(): Promise<void> {
	try {
		const location: string = core.getInput('location');
		const framework = core.getInput('framework').toLowerCase();
		const summarizerMethod = SUMMARIZER_BY_FRAMEWORK.get(framework);
		if (!summarizerMethod) {
			throw new Error(`Could not find summarizer method for framework ${framework}`);
		}
		const failingClasses: ClassDescriptor[] = await summarizerMethod(location);
		const summary = core.summary
			.addHeading(core.getInput('project-name'))
			.addRaw(`Summary derived from data in ${core.getInput('artifact-name')}.`)
		if (failingClasses.length === 0) {
			summary.addRaw(UNKNOWN_FAILURE_MESSAGE);
		} else {
			for (const failingClass of failingClasses) {
				summary.addHeading(failingClass.file, 2);
				for (const failingTest of failingClass.failures) {
					summary.addDetails(failingTest.test, failingTest.failure);
				}
			}
		}
		await summary.write();
	} catch (error) {
		if (error instanceof Error) {
			core.setFailed(`fail: ${error.message}`);
		} else {
			core.setFailed(`fail: ${error as string}`);
		}
	}
}

void run();

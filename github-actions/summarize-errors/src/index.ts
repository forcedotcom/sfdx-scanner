import core = require("@actions/core");
import {summarizeErrors} from "./summarizeJUnitErrors";

async function run(): Promise<void> {
	try {
		const location: string = core.getInput('location');
		const failures: string[] = await summarizeErrors(location);
		for (const failure of failures) {
			core.error(failure);
		}
	} catch (error) {
		if (error instanceof Error) {
			core.setFailed(`fail: ${error.message}`);
		} else {
			core.setFailed(`fail: ${error as string}`);
		}
	}
}

void run();

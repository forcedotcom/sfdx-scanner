import core = require("@actions/core");
import {summarizeJUnitErrors} from "./summarizeJUnitErrors";

async function run(): Promise<void> {
	try {
		const location: string = core.getInput('location');
		const files: string[] = await summarizeJUnitErrors(location);
		for (const file of files) {
			core.error(`There was a failure in ${file}`);
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

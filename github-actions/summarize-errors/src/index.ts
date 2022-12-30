import core = require("@actions/core");

function run(): void {
	try {
		const location = core.getInput('location');
		core.error(`beep boop ${location}`);
	} catch (error) {
		if (error instanceof Error) {
			core.setFailed(`fail: ${error.message}`);
		} else {
			core.setFailed(`fail: ${error as string}`);
		}
	}
}

run();

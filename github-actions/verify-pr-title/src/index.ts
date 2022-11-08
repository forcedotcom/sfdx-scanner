import core = require("@actions/core");
import github = require("@actions/github");
import { verifyPRTitleForBugId } from "./verifyPrTitle";
import { verifyPRTitleForBadTitle } from "./verifyPrTitle";

/**
 * Verifies that a pull request title conforms to the pattern required by git2gus
 * Also checks that the title does not start with d/ or r/
 */
function run(): void {
	try {
		const pullRequest = github.context.payload.pull_request;

		// Verify that this action was configured against a pull request
		if (!pullRequest) {
			core.setFailed(`This action only supports pull requests.`);
			return;
		}

		// Examine the title for the expected patterns
		const title = pullRequest.title;
		console.log(`Keys are ${Object.keys(pullRequest)}`);
		console.log(`Base is ${Object.keys(pullRequest.base)}`);
		console.log(`Base ref is ${pullRequest.base.ref}`);
		console.log(`Base label is ${pullRequest.base.label}`);
		if (verifyPRTitleForBugId(title) && verifyPRTitleForBadTitle(title)) {
			console.log(`PR Title '${title}' accepted.`);
		} else {
			core.setFailed(
				`PR Title '${title}' is missing a valid GUS work item OR it starts with d/ or r/`
			);
			return;
		}
	} catch (error) {
		core.setFailed(error.message);
	}
}

run();

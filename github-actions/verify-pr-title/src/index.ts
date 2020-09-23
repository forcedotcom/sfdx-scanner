import core = require("@actions/core");
import github = require("@actions/github");
import { verifyPRTitle } from "./verifyPrTitle";

/**
 * Verifies that a pull request title conforms to the pattern required by git2gus.
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
		if (verifyPRTitle(title)) {
			console.log(`PR Title '${title}' accepted.`);
		} else {
			core.setFailed(
				`PR Title '${title}' is missing a valid GUS work item.`
			);
			return;
		}
	} catch (error) {
		core.setFailed(error.message);
	}
}

run();

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

		// Examine the title and base branch for the expected patterns
		const title = pullRequest.title as string;
		if (verifyPRTitleForBugId(title) && verifyPRTitleForBadTitle(title)) {
			console.log(`PR Title '${title}' accepted.`);
		} else {
			core.setFailed(
				`PR Title '${title}' is missing a valid GUS work item OR it starts with d/ or r/`
			);
			return;
		}
	} catch (error) {
		if (error instanceof Error) {
			core.setFailed(error.message);
		} else {
			core.setFailed(error as string);
		}
	}
}

run();

import core = require("@actions/core");
import github = require("@actions/github");
import { verifyDevBranchPrTitle, verifyReleaseBranchPrTitle } from "./verifyPrTitle";

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

		// Examine the title for the expected patterns, which vary depending on what the target branch is.
		const title = pullRequest.title;
		const targetBranch = pullRequest.base.ref;
		if (targetBranch === "release" || targetBranch === "documentation") {
			// Release branches have their own less stringent format.
			if (verifyReleaseBranchPrTitle(title)) {
				console.log(`PR title '${title}' accepted for release branch.`);
			} else {
				core.setFailed(
					`PR title '${title}' does not match the release PR template of "RELEASE: @W-XXXX@: Summary".`
				);
				return;
			}
		} else if (targetBranch === "dev" || targetBranch === "docdev") {
			// Dev branches have a more stringent format.
			if (verifyDevBranchPrTitle(title)) {
				console.log(`PR title '${title}' accepted for dev branch.`);
			} else {
				core.setFailed(
					`PR title '${title}' does not match the dev PR template of "TYPE (SCOPE): @W-XXXX@: Summary"`
				);
				return;
			}
		} else {
			// Not sure why you'd have a pull request aimed at some other branch, but that should probably be allowed.
			console.log(`PR title '${title}' automatically accepted for non-release, non-dev branch`);
		}
	} catch (error) {
		core.setFailed(error.message);
	}
}

run();

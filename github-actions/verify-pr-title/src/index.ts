import core = require("@actions/core");
import github = require("@actions/github");
import {verifyReleasePrTitle} from "./verifyReleasePrTitle";
import {verifyMain2DevPrTitle} from "./verifyMain2DevPrTitle";
import {verifyFeaturePrTitle} from "./verifyFeaturePrTitle";

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
		const title = pullRequest.title as string;
		const base = pullRequest.base as {ref: string};
		const baseBranch = base.ref;
		const head = pullRequest.head as {ref: string};
		const headBranch = head.ref;
		if (headBranch.startsWith("m2d/") && baseBranch === "dev") {
			// "m2d/" is the prefix of the auto-generated branches we use to merge `main` into `dev` post-release.
			// Pull Requests merging these branches into `dev` have their own title convention separate from
			// the convention for other aimed-at-`dev` PRs.
			if (verifyMain2DevPrTitle(title)) {
				console.log(`PR title '${title}' accepted for dev branch.`);
			} else {
				core.setFailed(
					`PR title '${title}' does not match the template of "Main2Dev @W-XXXX@ Merging after vX.Y.Z"`
				);
				return;
			}
		} else if (baseBranch === "release" || baseBranch === "main") {
			// There's a title convention for merging PRs into `release`/`main`.
			if (verifyReleasePrTitle(title)) {
				console.log(`PR title '${title}' accepted for ${baseBranch} branch`);
			} else {
				core.setFailed(
					`PR title '${title}' does not match the template of "RELEASE @W-XXXX@ Summary"`
				);
				return;
			}
		} else if (baseBranch == "dev" || /^release-\d+\.\d+\.\d+\.*$/.test(baseBranch)) {
			// There's a title convention for merging feature branch PRs into `dev` or `release-X.Y.Z`
			// branches.
			if (verifyFeaturePrTitle(title)) {
				console.log(`PR title '${title}' accepted for ${baseBranch} branch`);
			} else {
				core.setFailed(
					`PR title '${title}' does not match the template of "NEW|FIX|CHANGE (__) @W-XXXX@ Summary" or "NEW|FIX|CHANGE @W-XXXX@ Summary"`
				);
				return;
			}
		} else {
			// For PRs aimed at any other branch, anything goes.
			console.log(`PR title '${title}' automatically accepted for ${baseBranch} branch`);
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

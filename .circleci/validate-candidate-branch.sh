#!/bin/bash
# This script is intended for use in CircleCI in a Linux executor. It accepts the name of a github branch, and verifies
# that the branch is an acceptable candidate for publishing to NPM.

set -e
SCANNER_BRANCH=$1

# First, we need to make sure that the name of the branch matches the pattern we've established.
# i.e., `v3-X-Y`, where X and Y are numbers.
# Note: [[:digit:]] is the bash-equivalent of the \d special character.
[[ ${SCANNER_BRANCH} =~ ^v3-[[:digit:]]+-[[:digit:]]+$ ]] || (echo "Branch must be of format 'v3-X-Y', where X and Y are numbers" && exit 1)

# Next, we need to make sure that the branch's name matches the version defined in the package.json. Do that by...
# - Logging the package.json with `cat`
# - Feeding that through `jq` and pulling out the "version" property
# - Replacing the dots in the version property with dashes.
# - Using xargs to strip off the leading and trailing quotes.
# - Tacking a 'v' onto the start.
PACKAGE_STRING=v`cat package.json | jq '.version' | tr . - | xargs`
[[ ${SCANNER_BRANCH} == ${PACKAGE_STRING} ]] || (echo "Branch name must match version defined in package.json" && exit 1)

# Finally, we need to make sure that the branch has all of the same commits as the `release` branch.
# Fetch release from origin, since we might not necessarily have it yet.
git fetch origin release-3
# Also fetch the branch we were given. This shouldn't be strictly necessary, but it lets us run the script against branches
# beyond the current branch.
git fetch origin ${SCANNER_BRANCH}

# Compare the commits on both branches by using the `git log` command, then pipe that into `wc` to get the character count,
# and use xargs to trim white space.
DIFFERENCE=`git diff release..${SCANNER_BRANCH} | wc -m | xargs`
[[ ${DIFFERENCE} == 0 ]] || (echo "Commits on branch must exactly match those on 'release-3' branch." && exit 1)

# If we're here, then the branch is acceptable.
echo "The branch appears publishable."
exit 0

#!/bin/bash
# This script is intended for use in a Linux Docker image running on CircleCI. It will upgrade nodejs to the version with
# the specified alias (i.e., "current" or "lts").
# Running this script locally is STRONGLY DISCOURAGED. Please don't do it.

set -e
VERSION_ALIAS=$1

# Install `n`, a module that we can use to resolve the node alias into an actual version.
npm install n

# We'll want to de-alias the desired version, prefix it with a 'v', and use it to construct the name of the tar we want.
DESIRED_VERSION=v`npx n ls-remote ${VERSION_ALIAS}`
DESIRED_FILE="node-${DESIRED_VERSION}-linux-x64"

# Download both the desired tar and the associated checksum file.
# Note about flags: -s == --silent, -S == --show-error, -L == --location (follows redirects)
curl -sSL -O https://nodejs.org/dist/${DESIRED_VERSION}/${DESIRED_FILE}.tar.xz
curl -O https://nodejs.org/dist/${DESIRED_VERSION}/SHASUMS256.txt

# Validate the tar using the checksum.
grep ${DESIRED_FILE}.tar.xz SHASUMS256.txt | sha256sum -c -

# Extract the node executable.
sudo tar --strip-components=2 -xJ -C /usr/local/bin/ ${DESIRED_FILE}/bin/node -f ${DESIRED_FILE}.tar.xz

# Install NPM using the locally-copied install script
sudo .circleci/npm-install.sh

# Delete the downloaded files, since we no longer need them.
rm -f ${DESIRED_FILE}.tar.xz
rm -f SHASUMS256.txt

# Verify that we're now using the correct version of node.
if [[ `node -v` != "${DESIRED_VERSION}" ]]; then
	echo Still using wrong node version. Expected ${DESIRED_VERSION}, got `node -v`
	exit -1
fi

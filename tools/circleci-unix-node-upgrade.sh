#!/bin/bash
# This script is intended for use in a Linux Docker image running on CircleCI. It will upgrade nodejs to the version with
# the specified alias (i.e., "current" or "lts").
# Running this script locally is STRONGLY DISCOURAGED. Please don't do it.

set -e
VERSION_ALIAS=$1

# Install `n`, a module that we can use to resolve the node alias into an actual version.
npm install n

# De-alias the desired version. Prefix it with a 'v', since it doesn't have that already and we need it to have that.
DESIRED_VERSION=v`npx n ls-remote ${VERSION_ALIAS}`

# Install the desired version.
curl -sSL "https://nodejs.org/dist/${DESIRED_VERSION}/node-${DESIRED_VERSION}-linux-x64.tar.xz" | sudo tar --strip-components=2 -xJ -C /usr/local/bin/ node-${DESIRED_VERSION}-linux-x64/bin/node

# Install NPM
curl https://www.npmjs.com/install.sh | sudo bash

# Verify that we're using the correct node version.
if [ `node -v` != "${DESIRED_VERSION}" ]; then
	echo Still using the wrong node version. Expected ${DESIRED_VERSION}, got `node -v`
	exit -1
fi

#!/bin/bash
# This script is intended for use in a Windows Executor running on CircleCI. It will upgrade nodejs to the version with
# the specified alias (i.e. "current" or "lts").
# Running this script locally is STRONGLY DISCOURAGED. Please don't do it.

set -e
VERSION_ALIAS=$1

# nvm, aka node version manager, is pre-installed on Windows executors. We'll use it to de-alias the version.
# The output of the `list available` command is a weird ascii-formatted chart that looks like the one below. We want the
# first actual line of data.
# |   CURRENT    |     LTS      |  OLD STABLE  | OLD UNSTABLE |
# |--------------|--------------|--------------|--------------|
# |    16.4.2    |   14.17.3    |   0.12.18    |   0.11.16    | <= We want this line!
# |    16.4.1    |   14.17.2    |   0.12.17    |   0.11.15    |
VERSION_CHART=`nvm list available | sed -n 4p`

# The cells in the chart are delineated by the '|' character, so we want to split it using that character.
IFS='|' read -ra VERSION_ARRAY <<< ${VERSION_CHART}

# Depending on the alias, we want either the first or second entry in the chart.
if [[ "${VERSION_ALIAS}" = "current" ]]; then
	WANTED_VERSION=${VERSION_ARRAY[1]}
elif [[ "${VERSION_ALIAS}" = "lts" ]]; then
	WANTED_VERSION=${VERSION_ARRAY[2]}
else
	echo "Please specify either 'lts' or 'current' for the parameter."
	exit -1
fi


# The version is going to have some leading and trailing spaces, so we want to strip those away.
WANTED_VERSION=`echo ${WANTED_VERSION}| sed 's/ *$//g'`

# Install and switch to the desired version
nvm install ${WANTED_VERSION}
nvm use ${WANTED_VERSION}

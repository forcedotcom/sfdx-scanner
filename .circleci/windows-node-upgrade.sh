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

# The Windows orb contains nvm-windows 1.1.7, this has a known bug with npm 8.3.1.
# Workaround the issue by recreating the symlinks as described here https://github.com/npm/cli/issues/4340#issuecomment-1025833090
cd "C:\ProgramData\nvm\v${WANTED_VERSION}\node_modules\npm\node_modules\@npmcli"
rm arborist
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/arborist" .

cd "C:\ProgramData\nvm\v${WANTED_VERSION}\node_modules\npm\node_modules"
rm libnpmversion
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmversion" .
rm libnpmteam
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmteam" .
rm libnpmsearch
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmsearch" .
rm libnpmpublish
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmpublish" .
rm libnpmpack
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmpack" .
rm libnpmorg
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmorg" .
rm libnpmhook
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmhook" .
rm libnpmfund
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmfund" .
rm libnpmexec
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmexec" .
rm libnpmdiff
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmdiff" .
rm libnpmaccess
ln -s "/c/Program Files/nodejs/node_modules/npm/workspaces/libnpmaccess" .

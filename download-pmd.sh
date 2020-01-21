#!/usr/bin/env bash

# Script to download PMD binary based on instructions from https://pmd.github.io

# TODO: 1. Get PMD_VERSION from package.json
# TODO: 2. What happens when pmd binary cannot be downloaded?

PMD_VERSION=6.20.0
PMD_FILE=pmd-bin-${PMD_VERSION}
PMD_URL=https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/${PMD_FILE}.zip
DIST_DIR=`pwd`/dist
PMD_ZIP=${DIST_DIR}/${PMD_FILE}.zip
PMD_INITIAL_DIR=${DIST_DIR}/${PMD_FILE}
PMD_DIR=${DIST_DIR}/pmd

echo "Cleaning up dist"
rm -rf ${DIST_DIR}

echo "Creating directory ${DIST_DIR}"
mkdir -p ${DIST_DIR}

echo "Downloading PMD binary. . ."
curl -L -o ${PMD_ZIP} ${PMD_URL}

if [ -f ${PMD_ZIP} ]; then
    echo "PMD binary has been downloaded successfully."
    echo "Unpackaging PMD binary to ${DIST_DIR}"
    unzip ${PMD_ZIP} -d ${DIST_DIR}
    mv ${PMD_INITIAL_DIR} ${PMD_DIR}
else
    echo "Download failed. Could not setup PMD."
    echo "Plugin setup was unsuccessful."
    exit 2
fi

#check JAVA_HOME
echo "Checking if JAVA_HOME has been set. . ."
if [[ -z ${JAVA_HOME} ]]; then
    echo "Please set JAVA_HOME value as an env variable. JDK version 1.8 and later works best."
    exit 3;
else
    echo "JAVA_HOME=${JAVA_HOME}"
fi

echo "Plugin setup completed successfully."
exit 0
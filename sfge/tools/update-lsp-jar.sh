#!/usr/bin/env bash
#
# Downloads the latest apex-jorje-lsp.jar file from forcedotcom/salesforcedx-vscode
#  and extracts classes needed by SFGE into a new jar.
#
# It extracts the following files
# - All files in the apex directory
# - A subset of files that correspond to the com.google.common.collect package
# - messages_en_US.properties
#
# This script creates files in the OS temp directory. They will be cleaned up when the OS reboots
#
# Requirements: realpath - (macOS) install with "brew install coreutils"
#
# Usage: ./update-lsp.jar.sh
#
#

# Exit on error
set -e

# Exit on unset variables
set -u

# Obtain the directory that the script was run from
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

# Location of the apex-jorje-lsp.jar on github
GITHUB_JAR_FILE_LOCATION="https://raw.githubusercontent.com/forcedotcom/salesforcedx-vscode/develop/packages/salesforcedx-vscode-apex/out/apex-jorje-lsp.jar"

# Java classes written by the Apex team that are in the com.google.common.collect package
# These are copied over to the new jar
CUSTOM_COLLECT_CLASSES=( "ConcatenatedLists" "MoreLists" "MoreMaps" "MoreSets" "PairList" "SingleAppendList" "SinglePrependList" )

# -d directory
# -t create the temp directory in the OS temp directory instead of the working directory
TMP_DIR=$(mktemp -d -t update.lsp.jar.XXXXXXXXXX)
echo "*** Created temp directory ${TMP_DIR} ***"

# Location of the downloaded jar file
ORIGINAL_JAR_FILE="${TMP_DIR}/apex-jorje-lsp.jar"

# Path within the jar file that contains the com.google.common.collect package
COLLECT_PATH="com/google/common/collect"

# Location of the MANIFEST.MF file used to create the jar
MANIFEST_FILE="${TMP_DIR}/MANIFEST.MF"

# Directory which receives files necessary for the sfge jar
SFGE_JAR_DIR="${TMP_DIR}/sfge-jar"

# Directory which receives com.google.common.collect package files necessary for the sfge jar
SFGE_JAR_COLLECT_DIR="${SFGE_JAR_DIR}/${COLLECT_PATH}"

# Location of the sfge jar file
SFGE_JAR_JAR_FILE=$(realpath "${SCRIPT_DIR}/../lib/apex-jorje-lsp-sfge.jar")

# Directory where the files from the original jar are placed
UNZIPPED_DIR="${TMP_DIR}/unzipped"

# Directory which contains the com.google.common.collect package files necessary for the sfge jar
UNZIPPED_COLLECT_DIR="${UNZIPPED_DIR}/${COLLECT_PATH}"

echo "*** Downloading jar file ***"
curl --output "${ORIGINAL_JAR_FILE}" "${GITHUB_JAR_FILE_LOCATION}"

echo "*** Unzipping ${ORIGINAL_JAR_FILE} to ${UNZIPPED_DIR}***"
unzip "${ORIGINAL_JAR_FILE}" -d "${UNZIPPED_DIR}"

echo "*** Creating ${MANIFEST_FILE} ***"
echo "Manifest-Version: 1.0" > "${MANIFEST_FILE}"

echo "*** Creating ${SFGE_JAR_DIR} ***"
mkdir "${SFGE_JAR_DIR}"

echo "*** Copying Apex messages file ***"
cp -a -v "${UNZIPPED_DIR}/messages_en_US.properties" "${SFGE_JAR_DIR}"

echo "*** Copying Apex files ***"
cp -a -v "${UNZIPPED_DIR}/apex" "${SFGE_JAR_DIR}"

echo "*** Copying common.collect files ***"
mkdir -p "${SFGE_JAR_DIR}/com/google/common/collect"

for class in "${CUSTOM_COLLECT_CLASSES[@]}"
do
    # Copy the outer class file
    cp -a -v "${UNZIPPED_COLLECT_DIR}/${class}.class" "${SFGE_JAR_COLLECT_DIR}"

    # Copy any inner classes if they exist
    # -r do not allow backslashes to escape any characters
    find "${UNZIPPED_COLLECT_DIR}" -name "${class}\$*.class" | while read -r full_file_path
    do
        cp -a -v "${full_file_path}" "${SFGE_JAR_COLLECT_DIR}"
    done
done


echo "*** Creating ${SFGE_JAR_JAR_FILE} ***"
(cd "${SFGE_JAR_DIR}" && jar cmf "${MANIFEST_FILE}" "${SFGE_JAR_JAR_FILE}" .)

echo "*** SUCCESS: Please commit ${SFGE_JAR_JAR_FILE} ***"


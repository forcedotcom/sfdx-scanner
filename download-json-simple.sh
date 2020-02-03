#!/usr/bin/env bash

# Script to download json-simple binary.

# TODO: 1. Get JSON_SIMPLE_VERSION from package.json
# TODO: 2. What happens when binary cannot be downloaded?


usage(){
	echo "
	usage: $0 [options]
	$0 downloads and sets up json-simple
	OPTIONS:
    -h | --help         Shows the usage information
    -f | --force        Forces download of JAR. Default behavior is to download/setup JAR only if needed
	"
}

FORCE=false
REQUIRES_SETUP=true

# Parse arguments
while [ "$1" != "" ]
do
	case $1 in
		-h | --help ) usage
		exit 0
		;;

		-f | --force ) shift
		FORCE=true
		;;

		*	) usage
		exit 2
		;;

	esac
	shift
done


JSON_VERSION=1.1.1
JSON_FILE=json-simple-${JSON_VERSION}.jar
JSON_URL=https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/json-simple/${JSON_FILE}
DIST_DIR=`pwd`/dist
JSON_DIR=${DIST_DIR}/json-simple
JSON_CHECKSUM=${DIST_DIR}/.json-checksum
CHECKSUM_CALC="tar -cf - \"${JSON_DIR}\" | md5"
JSON_PATH=${JSON_DIR}/${JSON_FILE}

# If there's an existing JSON-SIMPLE folder, we need to make sure it's valid.
if [ -d "${JSON_DIR}" ]; then
    echo "json-simple setup exists: ${JSON_DIR}"

    # Make sure the file isn't corrupted.
    if [[ -f "${JSON_CHECKSUM}" && `cat "${JSON_CHECKSUM}"` == `eval ${CHECKSUM_CALC}"` ]]; then
        echo "JAR contents look unchanged"
        REQUIRES_SETUP=false
    else
        echo "Information missing or JAR appears to be modified"
    fi
else
    echo "No setup found."
fi

# If we've already got a setup and we're not being forced to do another setup, we can just exit.
if [[ "${FORCE}" == "false" && "${REQUIRES_SETUP}" == "false" ]]; then
    echo "No download needed"
    exit 0
fi

# Unlike download-pmd.sh, on which this script is based, we won't delete the entire dist directory. We'll just delete anything
# that looks like it's related to json-simple.
echo "Cleaning up dist"
rm -rf ${JSON_DIR}
rm ${JSON_CHECKSUM}

echo "Creating directory ${JSON_DIR}"
mkdir -p ${JSON_DIR}

echo "Downloading json-simple JAR..."
curl -L -o ${JSON_PATH} ${JSON_URL}

if [ -f "${JSON_PATH}" ]; then
    echo "JSON binary successfully downloaded."
else
    echo "Download failed. Could not setup JSON-SIMPLE."
    echo "Plugin setup was unsuccessful."
    exit 2
fi

# Create checksum for verification later
echo `eval "{CHECKSUM_CALC}"` > "${JSON_CHECKSUM}"

# Check JAVA_HOME
echo "Checking if JAVA_HOME has been set..."
if [[ -z ${JAVA_HOME} ]]; then
    echo "Please set JAVA_HOME value as an env variable. JDK version 1.8 and later works best."
    exit 3
else
    echo "JAVA_HOME=${JAVA_HOME}"
fi

echo "Plugin setup completed successfully."
exit 0

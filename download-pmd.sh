#!/usr/bin/env bash

# Script to download PMD binary based on instructions from https://pmd.github.io

# TODO: 1. Get PMD_VERSION from package.json
# TODO: 2. What happens when pmd binary cannot be downloaded?


usage(){
	echo "
	usage: $0 [options]
	$0 downloads and sets up PMD
	OPTIONS:
    -h | --help         Shows the usage information
    -f | --force        Forces download of PMD. Default behavior is to download/setup PMD only if needed  
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



PMD_VERSION=6.20.0
PMD_FILE=pmd-bin-${PMD_VERSION}
PMD_URL=https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/${PMD_FILE}.zip
DIST_DIR=`pwd`/dist
PMD_ZIP=${DIST_DIR}/${PMD_FILE}.zip
PMD_INITIAL_DIR=${DIST_DIR}/${PMD_FILE}
PMD_DIR=${DIST_DIR}/pmd
PMD_CHECKSUM=${DIST_DIR}/.pmd-checksum
CHECKSUM_CALC="tar -cf - \"${PMD_DIR}\" | md5"

if [ -d "${PMD_DIR}" ]; then
    echo "PMD setup exists: ${PMD_DIR}"

    # Confirm that file is not corrupt
    if [[ -f "${PMD_CHECKSUM}" && `cat "${PMD_CHECKSUM}"` == `eval "${CHECKSUM_CALC}"` ]]; then
        echo "PMD contents look unchanged"
        REQUIRES_SETUP=false
    else
        echo "Information missing or PMD contents appear to have been modified"
    fi
else
    echo "No PMD setup found."
fi

if [[ "${FORCE}" == "false" && "${REQUIRES_SETUP}" == "false" ]]; then
    echo "No download or setup needed"
    exit 0
fi

echo "Cleaning up dist"
rm -rf ${DIST_DIR}

echo "Creating directory ${DIST_DIR}"
mkdir -p ${DIST_DIR}

echo "Downloading PMD binary. . ."
curl -L -o ${PMD_ZIP} ${PMD_URL}


if [ -f "${PMD_ZIP}" ]; then
    echo "PMD binary has been downloaded successfully."
    echo "Unpackaging PMD binary to ${DIST_DIR}"
    unzip ${PMD_ZIP} -d ${DIST_DIR}
    mv ${PMD_INITIAL_DIR} ${PMD_DIR}
else
    echo "Download failed. Could not setup PMD."
    echo "Plugin setup was unsuccessful."
    exit 2
fi

# Create checksum for verification later
echo `eval "${CHECKSUM_CALC}"` > "${PMD_CHECKSUM}"

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
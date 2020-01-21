#!/usr/bin/env bash

# Executes PMD with scanner's standard set of arguments
# TODO: 1. Reduce noise once this script stabilizes
# TODO: 2. Plan for exit codes on errors and what to do with them

# Usage information
usage(){
	echo "
	usage: $0 [options]
	$0 executes PMD
	OPTIONS:
    -h | --help 		Shows the usage information
    -R | --rulesets     Rulesets used for PMD run
    -d | --dir          Directory where code to be scanner resides
    -f | --format       Format in which report should be generated. csv, html, xml are recommended
    -r | --reportfile   Report file generated at the end of the run
	"
}


# Verify argument count
if [ $# -lt 4 ]
then
	echo "Invalid number of arguments."
	usage
	exit 2
fi

# Parse arguments
while [ "$1" != "" ]
do
	case $1 in
		-h | --help ) usage
		exit 0
		;;

		-R | --rulesets ) shift
		RULESET_LOCATION=$1
		;;

        -d | --dir ) shift
        CODE_DIR=$1
        ;;

        -f | --format ) shift
        OUTPUT_FORMAT=$1
        ;;

        -r | --reportfile ) shift
        REPORT_FILE=$1
        ;;

		*	) usage
		exit 2
		;;

	esac
	shift
done

PMD_RUN=`pwd`/dist/pmd/bin/run.sh


echo "About to invoke PMD with options:"
echo "Ruleset location: ${RULESET_LOCATION}"
echo "Code directory: ${CODE_DIR}"
echo "Output format: ${OUTPUT_FORMAT}"
echo "Report file: ${REPORT_FILE}"

#Execute PMD

${PMD_RUN} pmd \
-rulesets "${RULESET_LOCATION}" \
-dir "${CODE_DIR}" \
-format "${OUTPUT_FORMAT}" \
-reportfile "${REPORT_FILE}" \
-failOnViolation false

PMD_EXIT_CODE=$?

echo "Exit code: ${PMD_EXIT_CODE}"
exit ${PMD_EXIT_CODE}
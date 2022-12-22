module.exports = {
	"messages": {
		"pleaseWait": "Please wait",
		"spinnerStart": "Analyzing with Salesforce Graph Engine. See %s for details."
	},
	"errors": {
		"failedWithoutProjectDir": `Salesforce Graph Engine cannot run without --projectdir/-p. You must rerun your command, and either use --projectdir/-p so Graph Engine can run, or modify --engine/-e to exclude Graph Engine from execution.`
	},
	"warnings": {
		"skippedWithoutProjectDir": `Salesforce Graph Engine cannot run without --projectdir/-p, and will be skipped due to your %s.%s value of %s in %s.`
	}
};

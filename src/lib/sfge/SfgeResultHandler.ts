import { CommandLineResultHandler } from "../services/CommandLineResultHandler";
import { SfgeViolation } from "./SfgeWrapper";

export class SfgeResultHandler extends CommandLineResultHandler {


	
}

export function processStdout(output: string): SfgeViolation[] {
	// Pull the violation objects from the output.
	const violationsStartString = "VIOLATIONS_START";
	const violationsStart = output.indexOf(violationsStartString);
	if (violationsStart === -1) {
		return [];
	}
	const violationsEndString = "VIOLATIONS_END";
	const violationsEnd = output.indexOf(violationsEndString);
	const violationsJson = output.slice(violationsStart + violationsStartString.length, violationsEnd);
	const sfgeViolations: SfgeViolation[] = JSON.parse(violationsJson) as SfgeViolation[];

	return sfgeViolations;
}

export function processStderr(output: string): string {
	// We should handle errors by checking for our error start string.
	const errorStartString = "SfgeErrorStart\n";
	const errorStart = output.indexOf(errorStartString);
	if (errorStart === -1) {
		// If our error start string is missing altogether, then something went disastrously wrong, and we should
		// assume that the entire stderr is relevant.
		return output;
	} else {
		// If the error start string is present, it means we exited cleanly and everything prior to the string is noise
		// that can be omitted.
		return output.slice(errorStart + errorStartString.length);
	}
}
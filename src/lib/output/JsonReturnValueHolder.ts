import {AnyJson} from "@salesforce/ts-types";

/**
 * Container to hold the json return value for the --json flag used by some of the cli commands
 */
export class JsonReturnValueHolder {
	private jsonReturnValue: AnyJson;

	public set(jsonReturnValue: AnyJson): void {
		this.jsonReturnValue = jsonReturnValue;
	}

	public get(): AnyJson {
		return this.jsonReturnValue;
	}
}

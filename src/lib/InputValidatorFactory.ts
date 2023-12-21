import {InputValidator, RunCommandInputValidator, RunDfaCommandInputValidator} from "./InputValidator";
import {Display} from "./Display";

export interface InputValidatorFactory {
	createInputValidator(display: Display): InputValidator;
}

export class RunCommandInputValidatorFactory implements InputValidatorFactory {
	public createInputValidator(display: Display): InputValidator {
		return new RunCommandInputValidator(display);
	}
}

export class RunDfaCommandInputValidatorFactory implements InputValidatorFactory {
	public createInputValidator(display: Display): InputValidator {
		return new RunDfaCommandInputValidator(display);
	}
}

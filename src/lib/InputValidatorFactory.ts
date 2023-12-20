import {InputValidator, RunCommandInputValidator, RunDfaCommandInputValidator} from "./InputValidator";
import {Loggable} from "./Loggable";

export interface InputValidatorFactory {
	createInputValidator(uxLogger: Loggable): InputValidator;
}

export class RunCommandInputValidatorFactory implements InputValidatorFactory {
	public createInputValidator(uxLogger: Loggable): InputValidator {
		return new RunCommandInputValidator(uxLogger);
	}
}

export class RunDfaCommandInputValidatorFactory implements InputValidatorFactory {
	public createInputValidator(uxLogger: Loggable): InputValidator {
		return new RunDfaCommandInputValidator(uxLogger);
	}
}

/**
 * Builds expected env variables in this format:
 * SFDX_SCANNER.<ENGINE>.VARIABLE_NAME
 */

import { ENGINE } from "../../Constants";

const PREFIX = 'SFDX_SCANNER';
const SEPARATOR = '_';

export enum CONFIG_NAME {
	MINIMUM_TOKENS = 'Minimum_Tokens'
}

function getEnvVariableName(engine: ENGINE, configName: CONFIG_NAME): string {
	return `${PREFIX}${SEPARATOR}${engine.toUpperCase()}${SEPARATOR}${configName.toUpperCase()}`;
}

export function getEnvVariableAsString(engine: ENGINE, configName: CONFIG_NAME): string {
	const envVariableName = getEnvVariableName(engine, configName);
	return process.env[envVariableName];
}

export function getEnvVariableAsNumber(engine: ENGINE, configName: CONFIG_NAME): number {
	const envVariable = getEnvVariableAsString(engine, configName);
	if (envVariable) {
		// Clean up variable if it has any non-digit values
		return parseInt(envVariable.replace(/\D/g, ""), 10);
	}
	return undefined;
}

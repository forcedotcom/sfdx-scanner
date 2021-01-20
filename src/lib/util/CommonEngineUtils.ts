/*
 * Functions reused by various engines
 */

export function isValueInFilter(value: string, filterValues: string[]): boolean {
	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	return filterValues.some((myValue, index, array) => {
		return myValue === value;
	});
}

export function anyFilterValueStartsWith(startsWith: string, filterValues: string[]): boolean {
	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	return filterValues.some((value, index, array) => {
		return value.startsWith(startsWith);
	})
}

export function isFilterEmptyOrNameInFilter(value: string, filterValues: string[]): boolean {
	return filterValues.length === 0 || isValueInFilter(value, filterValues);
}

export function isFilterEmptyOrFilterValueStartsWith(startsWith: string, filterValues: string[]): boolean {
	return filterValues.length === 0 || anyFilterValueStartsWith(startsWith, filterValues);
}

export function isCustomRun(configName: string, engineOptions: Map<string, string>): boolean {
	return engineOptions.has(configName);
}


/**
 * Create a deep copy of an object by converting it to a JSON string and then parsing it.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const deepCopy = (obj: any): any => {
	return JSON.parse(JSON.stringify(obj));
}

const stringArrayTypeGuard = (object): object is string[] => {
	if (object == null || !Array.isArray(object)) {
		return false;
	}
	return !object.some(entry => typeof entry !== 'string');
}

export { deepCopy, stringArrayTypeGuard };

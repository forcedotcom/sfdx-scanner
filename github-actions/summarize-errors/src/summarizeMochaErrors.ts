import path = require('path');
import {xml2js, Element} from 'xml-js';
import {readFile} from "./fileUtils";
import {ClassDescriptor, TestDescriptor} from './types';

/**
 * Derive a descriptor for every class in the provided project folder that includes
 * failing tests.
 * @param projectFolder - The path to the test results for a Mocha run. Should contain a `mocha/test-results.xml` file.
 */
export async function summarizeErrors(projectFolder: string): Promise<ClassDescriptor[]> {
	const resultsPath = path.join(projectFolder, 'mocha', 'test-results.xml');
	const resultsXml = await readFile(resultsPath);

	const resultsJson: Element = xml2js(resultsXml, {
		compact: false,
		ignoreDeclaration: true
	}) as Element;
	const results: ClassDescriptor[] = [];
	const suitesWithFailures: Element[] = getTestSuitesWithFailures(resultsJson);
	// Since suites can share the same file name, we should map failure lists
	// by file to guarantee uniqueness.
	const failuresByFileName: Map<string, TestDescriptor[]> = new Map();
	for (const suite of suitesWithFailures) {
		const fileName = suite.attributes.file as string;
		const newFailures: TestDescriptor[] = getFailuresFromTestSuite(suite);
		const mappedFailures = failuresByFileName.get(fileName) || [];
		failuresByFileName.set(fileName, [...mappedFailures, ...newFailures]);
	}
	for (const [file, failures] of failuresByFileName.entries()) {
		results.push({
			file,
			failures
		});
	}
	return results;
}

/**
 * Given a JSON representing the root of a `test-results.xml` file, retrieves all elements
 * corresponding to failed <testsuite> tags.
 * @param root
 */
function getTestSuitesWithFailures(root: Element): Element[] {
	// The root should have a <testsuites> element with an attribute indicating how many
	// failures there were.
	const testSuites = root.elements[0];
	if (testSuites.name !== 'testsuites') {
		throw new Error(`Could not parse Mocha XML, expected 'testsuites' tag, received '${testSuites.name}'`);
	}
	// If there were no failures, we can return an empty list.
	if (testSuites.attributes.failures === 0) {
		return [];
	}
	// Otherwise, let's find the ones with failures and return those.
	return testSuites.elements.filter(t => t.attributes.failures > 0);
}

/**
 * Converts all <failure> tags inside the provided <testsuite> tag into TestDescriptors.
 * @param testSuite
 */
function getFailuresFromTestSuite(testSuite: Element): TestDescriptor[] {
	// Iterate over the test cases looking for ones that failed.
	const descriptors: TestDescriptor[] = [];
	for (const testCase of testSuite.elements) {
		// Skip any case without children.
		if (!testCase.elements || testCase.elements.length === 0) {
			continue;
		}
		for (const failure of testCase.elements) {
			// Skip any child of a test case that's not actually a failure.
			if (failure.name !== 'failure') {
				continue;
			}
			descriptors.push({
				test: testCase.attributes.classname as string,
				failure: getMessageFromFailure(failure)
			});
		}
	}
	return descriptors;
}

/**
 * Gets the message that should be used for a given failure.
 * @param failure
 */
function getMessageFromFailure(failure: Element): string {
	// If there are children, look for a CDATA whose content we can use.
	if (failure.elements && failure.elements.length > 0) {
		for (const el of failure.elements) {
			if (el.type === 'cdata') {
				return el.cdata;
			}
		}
	}
	// If we're here, we couldn't find a CDATA, so just use the failure tag's message.
	return `<code><pre>${failure.attributes.message as string}</pre></code>`;
}

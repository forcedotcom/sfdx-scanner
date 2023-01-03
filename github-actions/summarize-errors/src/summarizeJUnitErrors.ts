import path = require('path');
import * as JUnitUtils from './junitUtils';

/**
 * This chain of node expectations gets us from the root of the HTML file
 * to the declaration of `tab0`, the leftmost tab in the file. If there are
 * any failures, that's where they'll be.
 */
const TAB0_LOCATION_EXPECTATIONS: JUnitUtils.Expectation[] = [{
	type: "element",
	tagName: "html"
}, {
	type: "element",
	tagName: "body"
}, {
	type: "element",
	tagName: "div",
	id: "content"
}, {
	type: "element",
	tagName: "div",
	id: "tabs"
}, {
	type: "element",
	tagName: "div",
	id: "tab0"
}];

/**
 * If tab0 actually holds any failures, then it will have a descendent meeting these
 * expectations.
 */
const H2_FAILED_TESTS_EXPECTATIONS: JUnitUtils.Expectation[] = [{
	type: "element",
	tagName: "h2"
}, {
	type: "text",
	content: "Failed tests"
}];


const PATH_TO_JUNIT_REPORTS = ['build', 'reports', 'tests', 'test'];

export async function summarizeErrors(projectFolder: string): Promise<string[]> {
	// We want the index file for the JUnit run.
	const indexPath: string = path.join(projectFolder, ...PATH_TO_JUNIT_REPORTS, 'index.html');
	const indexJson: JUnitUtils.Node[] = await JUnitUtils.getJunitJson(indexPath);
	const classesWithFailures: string[] = getFailingClassNamesFromIndexFile(indexJson);
	const results: string[] = [];
	for (const cls of classesWithFailures) {
		const classPath: string = path.join(projectFolder, ...PATH_TO_JUNIT_REPORTS, 'classes', cls);
		const classJson: JUnitUtils.Node[] = await JUnitUtils.getJunitJson(classPath);
		const failures: string[] = getFailuresFromClassFile(classJson);
		results.push(`failures in ${cls}:\n${JSON.stringify(failures)}`);
	}
	return results;
}

function getFailingClassNamesFromIndexFile(indexJson: JUnitUtils.Node[]): string[] {
	// Get the tag for tab0, where failures will be if they exist at all.
	const tab0: JUnitUtils.Element = JUnitUtils.findChainedNode(indexJson, TAB0_LOCATION_EXPECTATIONS) as JUnitUtils.Element;
	// Make sure there are actually failures in this tab.
	if (!JUnitUtils.verifyNodeDescent(tab0, H2_FAILED_TESTS_EXPECTATIONS)) {
		// No errors to summarize, return empty list.
		return [];
	}
	// tab0 will have a `ul` child, with its own `li` children. Those are where the failures are.
	const ul: JUnitUtils.Element = JUnitUtils.findFirstMatchingNode(tab0.children, {
		type: "element",
		tagName: "ul"
	}) as JUnitUtils.Element;
	const listItems: JUnitUtils.Element[] = JUnitUtils.findAllMatchingNodes(ul.children, {
		type: "element",
		tagName: "li"
	}) as JUnitUtils.Element[];
	// For each list item, we want the `href` value in its first `a` tag.
	// Use a set to guarantee uniqueness
	const results: Set<string> = new Set();
	listItems.forEach(li => {
		const a = JUnitUtils.findFirstMatchingNode(li.children, {
			type: "element",
			tagName: "a"
		}) as JUnitUtils.Element;
		for (const attribute of a.attributes) {
			if (attribute.key === 'href') {
				results.add(attribute.value);
			}
		}
	});
	// Convert the set into an array, for ease of iteration.
	return [...results];
}

function getFailuresFromClassFile(classJson: JUnitUtils.Node[]): string[] {
	// Get the tag for tab0, where failures will be if they exist at all.
	const tab0: JUnitUtils.Element = JUnitUtils.findChainedNode(classJson, TAB0_LOCATION_EXPECTATIONS) as JUnitUtils.Element;
	// Make sure there are actually failures in this tab.
	if (!JUnitUtils.verifyNodeDescent(tab0, H2_FAILED_TESTS_EXPECTATIONS)) {
		// No errors to summarize, return empty list.
		return [];
	}
	// tab0 will have at least one `div` chid whose class is `test`
	const failures: JUnitUtils.Element[] = JUnitUtils.findAllMatchingNodes(tab0.children, {
		type: "element",
		tagName: "div",
		class: "test"
	}) as JUnitUtils.Element[];

	const results: string[] = [];
	for (const failure of failures) {
		const nameNode: JUnitUtils.Node = JUnitUtils.findChainedNode(failure.children, [{
			type: "element",
			tagName: "h3",
		}, {
			type: "text"
		}]);
		results.push((nameNode as JUnitUtils.Text).content);
	}
	return results;
}

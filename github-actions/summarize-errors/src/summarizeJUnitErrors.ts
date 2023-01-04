import path = require('path');
import {HimalayaNode, HimalayaElement, HimalayaExpectation, HimalayaText, ClassDescriptor, TestDescriptor} from './types';
import * as JUnitUtils from './junitUtils';

/**
 * This chain of node expectations gets us from the root of the HTML file
 * to the declaration of `tab0`, the leftmost tab in the file. If there are
 * any failures, that's where they'll be.
 */
const TAB0_LOCATION_EXPECTATIONS: HimalayaExpectation[] = [{
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
const H2_FAILED_TESTS_EXPECTATIONS: HimalayaExpectation[] = [{
	type: "element",
	tagName: "h2"
}, {
	type: "text",
	content: "Failed tests"
}];


const PATH_TO_JUNIT_REPORTS = ['build', 'reports', 'tests', 'test'];

export async function summarizeErrors(projectFolder: string): Promise<ClassDescriptor[]> {
	// We want the index file for the JUnit run.
	const indexPath: string = path.join(projectFolder, ...PATH_TO_JUNIT_REPORTS, 'index.html');
	const indexJson: HimalayaNode[] = await JUnitUtils.getJunitJson(indexPath);
	const classesWithFailures: string[] = getFailingClassNamesFromIndexFile(indexJson);
	const results: ClassDescriptor[] = [];
	for (const cls of classesWithFailures) {
		const classPath: string = path.join(projectFolder, ...PATH_TO_JUNIT_REPORTS, cls);
		const classJson: HimalayaNode[] = await JUnitUtils.getJunitJson(classPath);
		const failures: TestDescriptor[] = getFailuresFromClassFile(classJson);
		results.push({
			file: cls,
			failures
		});
	}
	return results;
}

function getFailingClassNamesFromIndexFile(indexJson: HimalayaNode[]): string[] {
	// Get the tag for tab0, where failures will be if they exist at all.
	const tab0: HimalayaElement = JUnitUtils.findChainedNode(indexJson, TAB0_LOCATION_EXPECTATIONS) as HimalayaElement;
	// Make sure there are actually failures in this tab.
	if (!JUnitUtils.verifyNodeDescent(tab0, H2_FAILED_TESTS_EXPECTATIONS)) {
		// No errors to summarize, return empty list.
		return [];
	}
	// tab0 will have a `ul` child, with its own `li` children. Those are where the failures are.
	const ul: HimalayaElement = JUnitUtils.findFirstMatchingNode(tab0.children, {
		type: "element",
		tagName: "ul"
	}) as HimalayaElement;
	const listItems: HimalayaElement[] = JUnitUtils.findAllMatchingNodes(ul.children, {
		type: "element",
		tagName: "li"
	}) as HimalayaElement[];
	// For each list item, we want the `href` value in its first `a` tag.
	// Use a set to guarantee uniqueness
	const results: Set<string> = new Set();
	listItems.forEach(li => {
		const a = JUnitUtils.findFirstMatchingNode(li.children, {
			type: "element",
			tagName: "a"
		}) as HimalayaElement;
		for (const attribute of a.attributes) {
			if (attribute.key === 'href') {
				results.add(attribute.value);
			}
		}
	});
	// Convert the set into an array, for ease of iteration.
	return [...results];
}

function getFailuresFromClassFile(classJson: HimalayaNode[]): TestDescriptor[] {
	// Get the tag for tab0, where failures will be if they exist at all.
	const tab0: HimalayaElement = JUnitUtils.findChainedNode(classJson, TAB0_LOCATION_EXPECTATIONS) as HimalayaElement;
	// Make sure there are actually failures in this tab.
	if (!JUnitUtils.verifyNodeDescent(tab0, H2_FAILED_TESTS_EXPECTATIONS)) {
		// No errors to summarize, return empty list.
		return [];
	}
	// tab0 will have at least one `div` chid whose class is `test`
	const failures: HimalayaElement[] = JUnitUtils.findAllMatchingNodes(tab0.children, {
		type: "element",
		tagName: "div",
		class: "test"
	}) as HimalayaElement[];

	const results: TestDescriptor[] = [];
	for (const failure of failures) {
		const nameNode: HimalayaText = JUnitUtils.findChainedNode(failure.children, [{
			type: "element",
			tagName: "h3",
		}, {
			type: "text"
		}]) as HimalayaText;
		const messageNode: HimalayaText = JUnitUtils.findChainedNode(failure.children, [{
			type: "element",
			tagName: "span",
			class: "code"
		}, {
			type: "element",
			tagName: "pre"
		}, {
			type: "text"
		}]) as HimalayaText;
		results.push({
			test: nameNode.content,
			failure: `<code><pre>${messageNode.content}</pre></code>`
		});
	}
	return results;
}

import path = require('path');
import {parse} from 'himalaya';
import {readFile} from './utils';

interface Node {
	type: string;
}

interface Attribute {
	key: string;
	value?: string;
}

interface Element extends Node {
	type: "element";
	tagName: string;
	children: Node[];
	attributes: Attribute[];
}

interface Text extends Node {
	type: "text";
	content: string;
}

interface Expectation {
	type: string;
	tagName?: string;
	id?: string;
	content?: string;
}

/**
 * This chain of node expectations gets us from the root of the HTML file
 * to the declaration of `tab0`, the leftmost tab in the file. If there are
 * any failures, that's where they'll be.
 */
const TAB0_LOCATION_EXPECTATIONS: Expectation[] = [{
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
const H2_FAILED_TESTS_EXPECTATIONS: Expectation[] = [{
	type: "element",
	tagName: "h2"
}, {
	type: "text",
	content: "Failed tests"
}];

export async function summarizeJUnitErrors(reportFolder: string): Promise<string[]> {
	// We want the index file.
	const indexPath: string = path.join(reportFolder, 'index.html');
	const indexContents: string = await readFile(indexPath);
	// eslint-disable-next-line @typescript-eslint/no-unsafe-call
	const indexJson: Node[] = parse(indexContents) as Node[];
	return getFailingTestNames(indexJson);
}

function getFailingTestNames(indexContents: Node[]): string[] {
	// Get the tag for tab0, where failures will be if they exist at all.
	const tab0: Element = findChainedNode(indexContents, TAB0_LOCATION_EXPECTATIONS) as Element;
	// Make sure there are actually failures in this tab.
	if (!verifyNodeDescent(tab0, H2_FAILED_TESTS_EXPECTATIONS)) {
		// No errors to summarize, return empty list.
		return [];
	}
	// tab0 will have a `ul` child, with its own `li` children. Those are where the failures are.
	const ul: Element = findFirstMatchingNode(tab0.children, {
		type: "element",
		tagName: "ul"
	}) as Element;
	const listItems: Element[] = findAllMatchingNodes(ul.children, {
		type: "element",
		tagName: "li"
	}) as Element[];
	// For each list item, we want the `href` value in its first `a` tag.
	return listItems.map(li => {
		const a = findFirstMatchingNode(li.children, {
			type: "element",
			tagName: "a"
		}) as Element;
		for (const attribute of a.attributes) {
			if (attribute.key === 'href') {
				return attribute.value;
			}
		}
	});
}

function findChainedNode(nodes: Node[], expectations: Expectation[]): Node {
	let nextNode: Element = null;
	let candidateNodes: Node[] = nodes;
	for (const expectation of expectations) {
		nextNode = findFirstMatchingNode(candidateNodes, expectation) as Element;
		candidateNodes = nextNode.children;
	}
	return nextNode;
}

function verifyNodeDescent(element: Element, expectations: Expectation[]): boolean {
	try {
		findChainedNode(element.children, expectations);
		// If we can find a node matching the expectations, we're good.
		return true;
	} catch {
		// If no node was found, an error will be thrown, meaning we're not good.
		return false;
	}
}

function findFirstMatchingNode(nodes: Node[], expectation: Expectation): Node {
	const allMatches = findAllMatchingNodes(nodes, expectation);
	if (allMatches.length === 0) {
		// If we're here, we couldn't find a node matching expectations.
		// Just throw an error.
		throw new Error(`Could not find node matching expectation ${JSON.stringify(expectation)}`);
	}
	return allMatches[0];
}

function findAllMatchingNodes(nodes: Node[], expectation: Expectation): Node[] {
	const matchingNodes: Node[] = [];
	for (const node of nodes) {
		// Make sure the types match.
		if (node.type !== expectation.type) {
			continue;
		}
		// If it's an element, validate it appropriately.
		if (node.type === 'element') {
			const element: Element = node as Element;
			// Make sure the tag name matches.
			if (element.tagName !== expectation.tagName) {
				continue;
			}
			// If ID provided, make sure that matches too.
			if (expectation.id) {
				let idMatches = false;
				for (const attribute of element.attributes) {
					if (attribute.key === 'id' && attribute.value === expectation.id) {
						idMatches = true;
						break;
					}
				}
				if (!idMatches) {
					continue;
				}
			}
		} else if (node.type === 'text') {
			// Validate a text node by checking its content.
			const text: Text = node as Text;
			if (text.content !== expectation.content) {
				continue;
			}
		}
		// If we're here, all appropriate checks were passed, so we can keep this node.
		matchingNodes.push(node);
	}
	return matchingNodes;
}

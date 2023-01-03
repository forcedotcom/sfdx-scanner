import {parse} from 'himalaya';
import {readFile} from './fileUtils';

export interface Node {
	type: string;
}

export interface Attribute {
	key: string;
	value?: string;
}

export interface Element extends Node {
	type: "element";
	tagName: string;
	children: Node[];
	attributes: Attribute[];
}

export interface Text extends Node {
	type: "text";
	content: string;
}

export interface Expectation {
	type: string;
	tagName?: string;
	id?: string;
	content?: string;
}

export async function getJunitJson(file: string): Promise<Node[]> {
	// eslint-disable-next-line @typescript-eslint/no-unsafe-call
	return parse(await readFile(file)) as Node[];
}


export function findChainedNode(nodes: Node[], expectations: Expectation[]): Node {
	let nextNode: Element = null;
	let candidateNodes: Node[] = nodes;
	for (const expectation of expectations) {
		nextNode = findFirstMatchingNode(candidateNodes, expectation) as Element;
		candidateNodes = nextNode.children;
	}
	return nextNode;
}

export function verifyNodeDescent(element: Element, expectations: Expectation[]): boolean {
	try {
		findChainedNode(element.children, expectations);
		// If we can find a node matching the expectations, we're good.
		return true;
	} catch {
		// If no node was found, an error will be thrown, meaning we're not good.
		return false;
	}
}

export function findFirstMatchingNode(nodes: Node[], expectation: Expectation): Node {
	const allMatches = findAllMatchingNodes(nodes, expectation);
	if (allMatches.length === 0) {
		// If we're here, we couldn't find a node matching expectations.
		// Just throw an error.
		throw new Error(`Could not find node matching expectation ${JSON.stringify(expectation)}`);
	}
	return allMatches[0];
}

export function findAllMatchingNodes(nodes: Node[], expectation: Expectation): Node[] {
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


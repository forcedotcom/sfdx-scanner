import {parse} from 'himalaya';
import {HimalayaNode, HimalayaElement, HimalayaExpectation, HimalayaText} from './types';
import {readFile} from './fileUtils';

export async function getJunitJson(file: string): Promise<HimalayaNode[]> {
	// eslint-disable-next-line @typescript-eslint/no-unsafe-call
	return parse(await readFile(file)) as HimalayaNode[];
}


export function findChainedNode(nodes: HimalayaNode[], expectations: HimalayaExpectation[]): HimalayaNode {
	let nextNode: HimalayaElement = null;
	let candidateNodes: HimalayaNode[] = nodes;
	for (const expectation of expectations) {
		nextNode = findFirstMatchingNode(candidateNodes, expectation) as HimalayaElement;
		candidateNodes = nextNode.children;
	}
	return nextNode;
}

export function verifyNodeDescent(element: HimalayaElement, expectations: HimalayaExpectation[]): boolean {
	try {
		findChainedNode(element.children, expectations);
		// If we can find a node matching the expectations, we're good.
		return true;
	} catch {
		// If no node was found, an error will be thrown, meaning we're not good.
		return false;
	}
}

export function findFirstMatchingNode(nodes: HimalayaNode[], expectation: HimalayaExpectation): HimalayaNode {
	const allMatches = findAllMatchingNodes(nodes, expectation);
	if (allMatches.length === 0) {
		// If we're here, we couldn't find a node matching expectations.
		// Just throw an error.
		throw new Error(`Could not find node matching expectation ${JSON.stringify(expectation)}`);
	}
	return allMatches[0];
}

export function findAllMatchingNodes(nodes: HimalayaNode[], expectation: HimalayaExpectation): HimalayaNode[] {
	const matchingNodes: HimalayaNode[] = [];
	for (const node of nodes) {
		// Make sure the types match.
		if (node.type !== expectation.type) {
			continue;
		}
		// If it's an element, validate it appropriately.
		if (node.type === 'element') {
			const element: HimalayaElement = node as HimalayaElement;
			// Make sure the tag name matches.
			if (element.tagName !== expectation.tagName) {
				continue;
			}
			// If we're looking for a matching class/id, we should do that.
			let matchingIdFound = false;
			let matchingClassFound = false;
			for (const attribute of element.attributes) {
				if (expectation.id && attribute.key === 'id' && attribute.value === expectation.id) {
					matchingIdFound = true;
				} else if (expectation.class && attribute.key === 'class' && attribute.value === expectation.class) {
					matchingClassFound = true;
				}
			}
			if (expectation.id && !matchingIdFound) {
				continue;
			} else if (expectation.class && !matchingClassFound) {
				continue;
			}
		} else if (node.type === 'text' && expectation.content) {
			// Validate a text node by checking its content.
			const text: HimalayaText = node as HimalayaText;
			if (text.content !== expectation.content) {
				continue;
			}
		}
		// If we're here, all appropriate checks were passed, so we can keep this node.
		matchingNodes.push(node);
	}
	return matchingNodes;
}


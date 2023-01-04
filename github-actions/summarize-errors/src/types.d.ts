export interface HimalayaNode {
	type: string;
}

export interface HimalayaAttribute {
	key: string;
	value?: string;
}

export interface HimalayaElement extends HimalayaNode {
	type: "element";
	tagName: string;
	children: HimalayaNode[];
	attributes: HimalayaAttribute[];
}

export interface HimalayaText extends HimalayaNode {
	type: "text";
	content: string;
}

export interface HimalayaExpectation {
	type: string;
	tagName?: string;
	class?: string;
	id?: string;
	content?: string;
}

export type TestDescriptor = {
	test: string;
	failure: string;
}

export type ClassDescriptor = {
	file: string;
	failures: TestDescriptor[];
}

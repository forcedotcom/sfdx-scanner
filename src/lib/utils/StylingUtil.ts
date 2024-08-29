import ansis from 'ansis';

/**
 * For now, the styling methods only accept objects if all of their keys correspond to string values. This puts the
 * burden of formatting non-string properties on the caller.
 */
type Styleable = null | undefined | {[key: string]: string};

export function toStyledHeaderAndBody(header: string, body: Styleable, keys?: string[]): string {
	const styledHeader: string = toStyledHeader(header);
	const styledBody: string = indent(toStyledPropertyList(body, keys));
	return `${styledHeader}\n${styledBody}`;
}

export function toStyledHeader(header: string): string {
	return `${ansis.dim('===')} ${ansis.bold(header)}`;
}

export function toComment(str: string): string {
	return ansis.dim(str);
}

export function toStyledPropertyList(body: Styleable, selectedKeys?: string[]): string {
	if (body == null || (selectedKeys && selectedKeys.length === 0)) {
		return '';
	}
	const keysToPrint = selectedKeys || [...Object.keys(body)];
	const longestKeyLength = Math.max(...keysToPrint.map(k => k.length));

	const styleProperty = (key: string, value: string): string => {
		const keyPortion = `${ansis.blue(key)}:`;
		const keyValueGap = ' '.repeat(longestKeyLength - key.length + 1);
		const valuePortion = value.replace('\n', `\n${' '.repeat(longestKeyLength + 2)}`);
		return `${keyPortion}${keyValueGap}${valuePortion}`;
	}

	const output = keysToPrint.map(key => styleProperty(key, body[key] || ''));
	return output.join('\n');
}

export function indent(text: string, indentLength: number = 4): string {
	return text.replace(/^/gm, ' '.repeat(indentLength));
}

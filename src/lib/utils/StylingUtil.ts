import ansis from 'ansis';

type Styleable = null | undefined | {[key: string]: string};

export function toStyledHeaderAndBody(header: string, body: Styleable, keys?: string[]): string {
	const styledHeader: string = toStyledHeader(header);
	const styledBody: string = toStyledBody(body, 4, keys);
	return `${styledHeader}\n${styledBody}`;
}

export function toStyledHeader(header: string): string {
	return `${ansis.dim('===')} ${ansis.bold(header)}`;
}

function toStyledBody(body: Styleable, indentSize: number, selectedKeys?: string[]): string {
	if (body == null || (selectedKeys != null && selectedKeys.length === 0)) {
		return '';
	}
	const indentation = ' '.repeat(indentSize);
	if (selectedKeys != null && selectedKeys.length === 0) {
		return '';
	}
	const keysToPrint = selectedKeys || [...Object.keys(body)];
	const longestKeyLength = Math.max(...keysToPrint.map(k => k.length));

	const styleProperty = (key: string, value: string): string => {
		const keyPortion = `${indentation}${ansis.blue(key)}:`;
		const keyValueGap = ' '.repeat(longestKeyLength - key.length + 1);
		const valuePortion = value.replace('\n', `\n${' '.repeat(indentSize + longestKeyLength + 2)}`);
		return `${keyPortion}${keyValueGap}${valuePortion}`;
	}

	const output = keysToPrint.map(key => styleProperty(key, body[key]));
	return output.join('\n');
}

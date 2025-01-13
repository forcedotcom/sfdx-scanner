import {Rule, SeverityLevel} from '@salesforce/code-analyzer-core';

export class StubRule1 implements Rule {
	public getName(): string {
		return 'StubRule1';
	}

	public getEngineName(): string {
		return 'FakeEngine1';
	}

	public getSeverityLevel(): SeverityLevel {
		return SeverityLevel.High;
	}

	public getFormattedSeverity(): string {
		return `2 (High)`;
	}

	public getFormattedType(): string {
		return "Standard";
	}

	public getTags(): string[] {
		return ['Recommended', 'Security'];
	}

	public getFormattedTags(): string {
		return "Recommended, Security";
	}

	public getDescription(): string {
		return 'This is the description for a stub rule. Blah blah blah.';
	}

	public getResourceUrls(): string[] {
		return ['www.google.com'];
	}

	public getFormattedResourceUrls(): string {
		return 'www.google.com';
	}
}

export class StubRule2 implements Rule {
	public getName(): string {
		return 'StubRule2';
	}

	public getEngineName(): string {
		return 'FakeEngine1';
	}

	public getSeverityLevel(): SeverityLevel {
		return SeverityLevel.Low;
	}

	public getFormattedSeverity(): string {
		return `4 (Low)`;
	}

	public getFormattedType(): string {
		return "Flow";
	}

	public getTags(): string[] {
		return ['CodeStyle', 'Performance'];
	}

	public getFormattedTags(): string {
		return "CodeStyle, Performance";
	}

	public getDescription(): string {
		return 'This is the description for a second stub rule. Blah blah blah.';
	}

	public getResourceUrls(): string[] {
		return ['www.bing.com', 'www.salesforce.com'];
	}

	public getFormattedResourceUrls(): string {
		return 'www.bing.com, www.salesforce.com';
	}
}

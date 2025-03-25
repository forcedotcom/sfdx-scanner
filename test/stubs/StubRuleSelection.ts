import { OutputFormat, Rule, RuleSelection } from '@salesforce/code-analyzer-core';

export class StubEmptyRuleSelection implements RuleSelection {
    getCount(): number {
        return 0;
    }
    getEngineNames(): string[] {
        return ['EmptyEngine'];
    }
    getRulesFor(_engineName: string): Rule[] {
        return [];
    }
    getRule(_engineName: string, _ruleName: string): Rule {
        throw new Error('Method not implemented.');
    }
    toFormattedOutput(format: OutputFormat): string {
        return `Rules formatted as ${format}`;
    }

}
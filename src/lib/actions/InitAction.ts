

export enum ConfigTemplate {
	EMPTY = 'empty',
	DEFAULT = 'default'
}

export type InitInput = {
	template: ConfigTemplate;
};

export class InitAction {
	public execute(input: InitInput): void {
	}
}

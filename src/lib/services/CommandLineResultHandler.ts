export type ResultHandlerArgs = {
	code: number;
	isSuccess: boolean;
	hasResults?: boolean;
	stdout: string;
	stderr: string;
	res: (string) => void;
	rej: (string) => void;
};

export class CommandLineResultHandler {
	public handleResults(args: ResultHandlerArgs): void {
		if (args.isSuccess) {
			args.res(args.stdout);
		} else {
			args.rej(args.stderr);
		}
	}
}

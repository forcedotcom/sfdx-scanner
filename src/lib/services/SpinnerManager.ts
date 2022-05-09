export interface SpinnerManager {
	startSpinner(): void;
	stopSpinner(success: boolean): void;
}

export class NoOpSpinnerManager implements SpinnerManager {
	public startSpinner(): void {
		// NO-OP
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public stopSpinner(success: boolean): void {
		// NO-OP
	}
}

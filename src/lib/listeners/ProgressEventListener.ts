import {
	CodeAnalyzer,
	EngineRunProgressEvent,
	EventType,
	RuleSelectionProgressEvent
} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {BundleName, getMessage} from '../messages';

export interface ProgressEventListener {
	listen(codeAnalyzer: CodeAnalyzer): void;
	stopListening(): void;
}

abstract class ProgressSpinner {
	private readonly display: Display;
	private readonly tickTime: number;
	private tickIntervalId: NodeJS.Timeout;
	private _isSpinning: boolean;
	private startTime: number;

	protected constructor(display: Display, tickTime: number = 1000) {
		this.display = display;
		this.tickTime = tickTime;
		this._isSpinning = false;
	}

	protected startSpinning(action: string): void {
		/* istanbul ignore if: Shouldn't be possible for spinner to start twice */
		if (this._isSpinning) {
			return;
		}
		this.startTime = Date.now();
		this.display.spinnerStart(action, this.createSpinnerStatus());
		if (this.tickTime > 0) {
			// `setInterval` means the callback will be called repeatedly. This allows us to automatically refresh the
			// spinner on a regular interval even if nothing happened. This is primarily useful for incrementing a timer,
			// so the user doesn't feel like the system is frozen.
			this.tickIntervalId = setInterval(() => {
				this.display.spinnerUpdate(this.createSpinnerStatus());
			}, this.tickTime);
		}
		this._isSpinning = true;
	}

	protected updateSpinner(): void {
		this.display.spinnerUpdate(this.createSpinnerStatus());
	}

	protected stopSpinning(): void {
		if (this.tickIntervalId) {
			clearInterval(this.tickIntervalId);
		}
		if (this._isSpinning) {
			this.display.spinnerStop(getMessage(BundleName.ProgressEventListener, 'base-spinner.done'));
			this._isSpinning = false;
		}
	}

	protected isSpinning(): boolean {
		return this._isSpinning;
	}

	/**
	 * Return the number of seconds (rounded down) that the spinner has been spinning.
	 * @protected
	 */
	protected getSecondsSpentSpinning(): number {
		return Math.floor((Date.now() - this.startTime) / 1000);
	}

	protected abstract createSpinnerStatus(): string;
}

export class RuleSelectionProgressSpinner extends ProgressSpinner implements ProgressEventListener {
	private isListening: boolean;
	private engineNames: string[];
	private completionPercent: number;

	/**
	 * @param display The display with which to show output to the user
	 * @param tickTime A number of milliseconds to wait between automatic ticks. Defaults to 1000. Negative values disable ticking.
	 */
	public constructor(display: Display, tickTime: number = 1000) {
		super(display, tickTime);
		this.isListening = false;
	}

	public listen(codeAnalyzer: CodeAnalyzer): void {
		/* istanbul ignore else: should not be calling listen() twice */
		if (!this.isListening) {
			this.isListening = true;
			this.engineNames = codeAnalyzer.getEngineNames();
			this.completionPercent = 0;
			codeAnalyzer.onEvent(EventType.RuleSelectionProgressEvent, (e: RuleSelectionProgressEvent) => this.handleEvent(e));
		}
	}

	public stopListening(): void {
		if (this.isListening) {
			this.isListening = false;
			this.stopSpinning();
		}
	}

	private handleEvent(e: RuleSelectionProgressEvent): void {
		if (!this.isListening) {
			return;
		}

		if (!this.isSpinning()) {
			this.startSpinning(getMessage(BundleName.ProgressEventListener, 'selection-spinner.action'));
		}

		this.completionPercent = e.percentComplete;
		this.updateSpinner();
		// Since the events this spinner listens to have one aggregated completion percentage, we can (and should) stop
		// listening as soon as we get the 100% event, instead of waiting for the `stopListening()` method.
		if (this.completionPercent === 100) {
			this.stopListening();
		}
	}

	protected createSpinnerStatus(): string {
		return getMessage(BundleName.ProgressEventListener, 'selection-spinner.status',
			[this.engineNames.join(', '), this.completionPercent, this.getSecondsSpentSpinning()]);
	}
}

export class EngineRunProgressSpinner extends ProgressSpinner implements ProgressEventListener {
	private isListening: boolean;
	/**
	 * Mapping from each engine's name to a number indicating its completion percentage.
	 * @private
	 */
	private progressMap: Map<string, number>;

	/**
	 * @param display The display with which to show output to the user
	 * @param tickTime A number of milliseconds to wait between automatic ticks. Defaults to 1000. Negative values disable ticking.
	 */
	public constructor(display: Display, tickTime: number = 1000) {
		super(display, tickTime);
		this.isListening = false;
	}

	public listen(codeAnalyzer: CodeAnalyzer): void {
		/* istanbul ignore else: should not be calling listen() twice */
		if (!this.isListening) {
			this.isListening = true;
			this.progressMap = new Map();
			codeAnalyzer.onEvent(EventType.EngineRunProgressEvent, (e: EngineRunProgressEvent) => this.handleEvent(e));
		}
	}

	public stopListening(): void {
		if (this.isListening) {
			this.isListening = false;
			this.stopSpinning();
		}
	}

	private handleEvent(e: EngineRunProgressEvent): void {
		if (!this.isListening) {
			return;
		}

		if (!this.isSpinning()) {
			this.startSpinning(getMessage(BundleName.ProgressEventListener, 'execution-spinner.action'));
		}

		this.progressMap.set(e.engineName, e.percentComplete);
		this.updateSpinner();
	}

	protected createSpinnerStatus(): string {
		const secondsRunning = this.getSecondsSpentSpinning();
		const totalEngines = this.progressMap.size;
		let unfinishedEngines = 0;
		const engineLines: string[] = [];
		for (const [name, progress] of this.progressMap.entries()) {
			if (progress !== 100) {
				unfinishedEngines += 1;
			}
			engineLines.push(getMessage(BundleName.ProgressEventListener, 'execution-spinner.engine-status', [name, progress]));
		}
		return [
			getMessage(BundleName.ProgressEventListener, 'execution-spinner.progress-summary', [unfinishedEngines, totalEngines, secondsRunning]),
			...engineLines
		].join('\n');
	}
}

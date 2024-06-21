import {
	CodeAnalyzer,
	EngineProgressEvent,
	EventType,
	RuleSelection
} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';
import {BundleName, getMessage} from '../messages';

export interface EngineProgressListener {
	listen(codeAnalyzer: CodeAnalyzer, ruleSelection: RuleSelection): void;
	stopListening(): void;
}

export class SpinnerProgressListener implements EngineProgressListener {
	private readonly display: Display;
	/**
	 * Mapping from each engine's name to a number indicating its completion percentage.
	 * @private
	 */
	private readonly progressMap: Map<string, number>;
	private startTime: number;
	private readonly tickTime: number;
	private tickIntervalId: NodeJS.Timeout;
	private isTicking: boolean;

	/**
	 *
	 * @param display The display with which to show output to the user.
	 * @param tickTime A number of milliseconds to wait between automatic ticks. Defaults to 1000. Negative values disable ticking.
	 */
	public constructor(display: Display, tickTime: number = 1000) {
		this.display = display;
		this.tickTime = tickTime;
		this.progressMap = new Map();
		this.isTicking = false;
	}

	public listen(codeAnalyzer: CodeAnalyzer, ruleSelection: RuleSelection): void {
		this.startTime = Date.now();
		this.startSpinning(ruleSelection);
		codeAnalyzer.onEvent(EventType.EngineProgressEvent, (e: EngineProgressEvent) => this.updateCorrespondingEngine(e));
	}

	public stopListening(): void {
		// Stop ticking.
		if (this.isTicking) {
			clearInterval(this.tickIntervalId);
			this.isTicking = false;
		}
		this.display.spinnerStop(getMessage(BundleName.SpinnerProgressListener, 'spinner.done'));
	}

	private startSpinning(ruleSelection: RuleSelection): void {
		for (const engineName of ruleSelection.getEngineNames()) {
			this.progressMap.set(engineName, 0);
		}
		const ruleCount = ruleSelection.getCount();
		this.display.spinnerStart(
			getMessage(BundleName.SpinnerProgressListener, 'spinner.message', [ruleCount, this.progressMap.size]),
			this.createSpinnerStatus()
		);
		if (this.tickTime > 0) {
			this.spinnerTick(this.tickTime);
		}
	}

	/**
	 * Using a regularly scheduled tick-event allows us to update the spinner on a regular interval even if nothing happens.
	 * We use this primarily to increment a timer so the user doesn't feel like the system is frozen.
	 * @param timeToNextTick
	 * @private
	 */
	private spinnerTick(timeToNextTick: number): void {
		// `setInterval` means the callback will be called repeatedly.
		this.tickIntervalId = setInterval(() => {
			this.display.spinnerUpdate(this.createSpinnerStatus());
		}, timeToNextTick);
		this.isTicking = true;
	}

	private updateCorrespondingEngine(event: EngineProgressEvent): void {
		this.progressMap.set(event.engineName, event.percentComplete);
		this.display.spinnerUpdate(this.createSpinnerStatus());
	}

	private createSpinnerStatus(): string {
		const secondsRunning = Math.floor((Date.now() - this.startTime) / 1000);
		const totalEngines = this.progressMap.size;
		let unfinishedEngines = 0;
		const engineLines: string[] = [];
		for (const [name, progress] of this.progressMap.entries()) {
			if (progress !== 100) {
				unfinishedEngines += 1;
			}
			engineLines.push(getMessage(BundleName.SpinnerProgressListener, 'spinner.engine-status', [name, progress]));
		}
		return [
			getMessage(BundleName.SpinnerProgressListener, 'spinner.progress-summary', [unfinishedEngines, totalEngines, secondsRunning]),
			...engineLines
		].join('\n');
	}
}


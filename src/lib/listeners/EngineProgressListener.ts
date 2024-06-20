import {
	CodeAnalyzer,
	EngineProgressEvent,
	EngineResultsEvent,
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
	private readonly tickTime: number;
	/**
	 * Mapping from each engine's name to a number indicating its completion percentage.
	 * @private
	 */
	private readonly progressMap: Map<string, number>;
	private startTime: number;
	private intervalId: NodeJS.Timeout;

	/**
	 *
	 * @param display The display with which to show output to the user.
	 * @param tickTime A number of milliseconds to wait between automatic ticks. Defaults to 1000. Negative values disable ticking.
	 */
	public constructor(display: Display, tickTime: number = 1000) {
		this.display = display;
		this.tickTime = tickTime;
		this.progressMap = new Map();
	}

	public listen(codeAnalyzer: CodeAnalyzer, ruleSelection: RuleSelection): void {
		this.startTime = Date.now();
		this.startSpinning(ruleSelection);
		codeAnalyzer.onEvent(EventType.EngineProgressEvent, e => this.updateCorrespondingEngine(e as EngineProgressEvent));
		codeAnalyzer.onEvent(EventType.EngineResultsEvent, e => this.completeCorrespondingEngine(e as EngineResultsEvent));
	}

	public stopListening(): void {
		// Stop ticking.
		clearInterval(this.intervalId);
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
		this.intervalId = setTimeout(() => {
			this.display.spinnerUpdate(this.createSpinnerStatus());
			this.spinnerTick(timeToNextTick);
		}, timeToNextTick)
	}

	private updateCorrespondingEngine(event: EngineProgressEvent): void {
		this.progressMap.set(event.engineName, event.percentComplete);
		this.display.spinnerUpdate(this.createSpinnerStatus());
	}

	private completeCorrespondingEngine(event: EngineResultsEvent): void {
		this.progressMap.set(event.results.getEngineName(), 100);
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


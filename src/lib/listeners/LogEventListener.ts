import {CodeAnalyzer, EngineLogEvent, EventType, LogEvent, LogLevel} from '@salesforce/code-analyzer-core';
import {Display} from '../Display';

export interface LogEventListener {
	listen(codeAnalyzer: CodeAnalyzer): void;
}

export class LogEventDisplayer implements LogEventListener {
	private readonly display: Display;

	public constructor(display: Display) {
		this.display = display;
	}

	public listen(codeAnalyzer: CodeAnalyzer): void {
		// Set up listeners
		codeAnalyzer.onEvent(EventType.LogEvent, (e: LogEvent) => this.handleEvent('Core', e));
		codeAnalyzer.onEvent(EventType.EngineLogEvent, (e: EngineLogEvent) => this.handleEvent(e.engineName, e));
	}

	private handleEvent(source: string, event: LogEvent|EngineLogEvent): void {
		// We've arbitrarily decided to log only events of type "Info" or higher, to avoid potentially flooding the CLI
		// with a ton of noisy statements from other engines. At some point in the future, we may make this configurable.
		if (event.logLevel > LogLevel.Info) {
			return;
		}
		const formattedMessage = `${source} [${formatTimestamp(event.timestamp)}]: ${event.message}`;
		switch (event.logLevel) {
			// We display errors at the "warning" level, because calling `.error()` would actually kill the transaction,
			// and we probably don't want that.
			case LogLevel.Error:
			case LogLevel.Warn:
				this.display.displayWarning(formattedMessage);
				return;
			case LogLevel.Info:
				this.display.displayInfo(formattedMessage);
				return;
		}
	}
}

function formatTimestamp(timestamp: Date): string {
	return `${timestamp.getHours()}:${timestamp.getMinutes()}:${timestamp.getSeconds()}`;
}

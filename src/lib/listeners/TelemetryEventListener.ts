import {
	CodeAnalyzer,
	EngineTelemetryEvent,
	EventType,
	TelemetryEvent
} from "@salesforce/code-analyzer-core";
import {TelemetryEmitter} from '../Telemetry';

export class TelemetryEventListener {
	private telemetryEmitter: TelemetryEmitter;

	public constructor(telemetryEmitter: TelemetryEmitter) {
		this.telemetryEmitter = telemetryEmitter;
	}

	public listen(codeAnalyzer: CodeAnalyzer): void {
		// Set up listeners.
		codeAnalyzer.onEvent(EventType.TelemetryEvent, (e: TelemetryEvent) => void this.handleEvent('Core', e));
		codeAnalyzer.onEvent(EventType.EngineTelemetryEvent,  (e: EngineTelemetryEvent) => void this.handleEvent(e.engineName, e));
	}

	public stopListening(): void {
		// Intentional no-op, because no cleanup is required.
	}

	private handleEvent(source: string, event: TelemetryEvent|EngineTelemetryEvent): Promise<void> {
		return this.telemetryEmitter.emitTelemetry(source, event.eventName, {
			...event.data,
			timestamp: event.timestamp.getTime(),
			uuid: event.uuid
		});
	}
}

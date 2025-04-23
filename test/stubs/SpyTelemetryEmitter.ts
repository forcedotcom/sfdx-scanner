import {TelemetryData} from '@salesforce/code-analyzer-core';
import {TelemetryEmitter} from '../../src/lib/Telemetry';

export type CapturedTelemetryEmission = {
	source: string,
	eventName: string,
	data: TelemetryData
};

export class SpyTelemetryEmitter implements TelemetryEmitter {
	private capturedTelemetry: CapturedTelemetryEmission[] = [];

	public emitTelemetry(source: string, eventName: string, data: TelemetryData): Promise<void> {
		this.capturedTelemetry.push({source, eventName, data});
		return Promise.resolve();
	}

	public getCapturedTelemetry(): CapturedTelemetryEmission[] {
		return this.capturedTelemetry;
	}
}

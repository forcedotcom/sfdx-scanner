import { Lifecycle } from '@salesforce/core';
import { TelemetryData } from '../../types';


export async function emitTelemetry(data: TelemetryData): Promise<void> {
	try {
		await Lifecycle.getInstance().emitTelemetry(data);
	} catch (e) {
		// Catch-block intentionally left empty to swallow error.
	}
}

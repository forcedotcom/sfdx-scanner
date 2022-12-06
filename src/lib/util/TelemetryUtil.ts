import { Lifecycle, Logger } from '@salesforce/core';
import { TelemetryData } from '../../types';

let LOGGER: Logger = null;

export async function emitTelemetry(data: TelemetryData): Promise<void> {
	try {
		await Lifecycle.getInstance().emitTelemetry(data);
	} catch (e) {
		// Log a warning about the failure to emit telemetry.
		LOGGER = LOGGER || await Logger.child('TelemetryUtil');
		LOGGER.warn(`Failed to emit telemetry event ${JSON.stringify(data)}`);
		// TODO: Consider displaying a verbose-only warning to the user.
	}
}

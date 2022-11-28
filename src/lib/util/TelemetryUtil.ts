import { Lifecycle, Logger, Messages } from '@salesforce/core';
import { TelemetryData } from '../../types';

let LOGGER: Logger = null;
Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages("@salesforce/sfdx-scanner", "TelemetryUtil");

export async function emitTelemetry(data: TelemetryData): Promise<void> {
	try {
		await Lifecycle.getInstance().emitTelemetry(data);
	} catch (e) {
		// Log a warning about the failure to emit telemetry.
		LOGGER = LOGGER || await Logger.child('TelemetryUtil');
		LOGGER.warn(messages.getMessage("telemetryEmitFailed", [JSON.stringify(data)]));
		// TODO: Consider displaying a verbose-only warning to the user.
	}
}

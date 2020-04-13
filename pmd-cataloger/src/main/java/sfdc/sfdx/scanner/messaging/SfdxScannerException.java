package sfdc.sfdx.scanner.messaging;

import com.google.common.base.Throwables;
import sfdc.sfdx.scanner.messaging.EventKey;

import java.util.Arrays;

/**
 * Internal exception representation.
 * Extends RuntimeException to avoid declaring everywhere
 * Handles capability to plug into SfdxMessager
 */
public class SfdxScannerException extends RuntimeException {

	private final EventKey eventKey;
	private final String[] args;

	public SfdxScannerException(EventKey eventKey, String... args) {
		this(eventKey, null, args);
	}

	public SfdxScannerException(EventKey eventKey, Throwable throwable, String... args) {
		super(throwable);

		this.eventKey = eventKey;
		this.args = args;
	}

	public EventKey getEventKey() {
		return eventKey;
	}

	public String[] getArgs() {
		return args;
	}

	public String getFullStacktrace() {
		return Throwables.getStackTraceAsString(this).replace("\\n", " | ");
	}

	@Override
	public String toString() {
		return "SfdxScannerException{" +
			"eventKey=" + eventKey +
			", args=" + Arrays.toString(args) +
			'}';
	}
}

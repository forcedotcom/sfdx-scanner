package sfdc.sfdx.scanner;

import com.google.common.base.Throwables;

import java.util.Arrays;
import java.util.List;

/**
 * Internal exception representation.
 * Extends RuntimeException to avoid declaring everywhere
 * Handles capability to plug into SfdxMessager
 */
public class SfdxScannerException extends RuntimeException {

  private final EventKey eventKey;
  private final List<String> args;

  public SfdxScannerException(EventKey eventKey, String... args) {
    this(eventKey, null, args);
  }

  public SfdxScannerException(EventKey eventKey, Throwable throwable, String... args) {
    super(throwable);

    // Confirm that the correct number of arguments for the message has been provided
    // If this fails, this would be a developer error
    assert (eventKey.getArgCount() == args.length);

    this.eventKey = eventKey;
    this.args = Arrays.asList(args);
  }

  public EventKey getEventKey() {
    return eventKey;
  }

  public List<String> getArgs() {
    return args;
  }

  public String getFullStacktrace() {
    return Throwables.getStackTraceAsString(this).replace("\\n", " | ");
  }

}

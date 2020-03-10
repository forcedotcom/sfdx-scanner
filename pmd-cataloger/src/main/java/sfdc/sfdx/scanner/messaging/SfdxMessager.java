package sfdc.sfdx.scanner.messaging;

import java.util.ArrayList;
import java.util.List;

public class SfdxMessager {
  // The START string gives us something to scan for when we're processing output.
  private static final String START = "SFDX-START";
  // The END string lets us know when a message stops, which should prevent bugs involving multi-line output.
  private static final String END = "SFDX-END";
  // These strings are all modifiers on the message.
  private static final String VERBOSE = "SFDX-VERBOSE";
  private static final String INFO = "SFDX-INFO";
  // Our choice of delimiter is (hopefully) distinctive enough that we shouldn't have to worry about bugs related to false-positives.
  private static final String DELIM = "::SFDX-DELIM::";

  private static SfdxMessager INSTANCE = null;

  public static SfdxMessager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SfdxMessager();
    }
    return INSTANCE;
  }

  public void info(String msg) {
    info(msg, false);
  }

  public void info(String msg, boolean verbose) {
    System.out.println(formatMessage(msg, INFO, verbose));
  }

  private String formatMessage(String msg, String type, boolean verbose) {
    // Create an array containing all of the components of our message.
    List<String> msgParts = new ArrayList<>();

    // The message must always start with the START string, so it can be immediately identified.
    msgParts.add(START);

    // If the message should only be displayed when the --verbose flag is present, the VERBOSE modifier comes next.
    if (verbose) {
      msgParts.add(VERBOSE);
    }

    // Next comes the type of message.
    msgParts.add(type);

    // Then the message itself.
    msgParts.add(msg);

    // Finally, the END string.
    msgParts.add(END);

    // We join everything together using our delimiter, and return it.
    return String.join(DELIM, msgParts);
  }
}

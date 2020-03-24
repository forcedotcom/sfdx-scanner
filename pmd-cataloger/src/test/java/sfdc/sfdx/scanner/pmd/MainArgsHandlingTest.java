package sfdc.sfdx.scanner.pmd;

import static org.junit.Assert.*;
import org.junit.Test;
import sfdc.sfdx.scanner.SfdxScannerException;
import sfdc.sfdx.scanner.EventKey;

import java.util.List;
import java.util.Map;

public class MainArgsHandlingTest {

  final Main main = new Main();

  @Test
  public void verifyHappyCase() {
    // Setup
    final String language = "apex";
    final String[] paths = {"/some/path1", "/some/other/path"};
    final String[] args = {language + Main.DIVIDER + String.join(Main.COMMA, paths)};

    // Run
    final Map<String, List<String>> stringListMap = main.parseArguments(args);

    // Validate
    assertEquals("Unexpected number of items in parsed map", 1, stringListMap.size());
    assertTrue("Language not found in parsed map", stringListMap.containsKey(language));

    final List<String> parsedPaths = stringListMap.get(language);
    assertEquals("Unexpected number of paths in parsed map", paths.length, parsedPaths.size());
    for(String path: paths) {
      assertTrue("Path not found in parsed map: " + path, parsedPaths.contains(path));
    }
  }

  @Test
  public void testWhenNoArgsProvided() {
    final String[] args = null;

    testParseArgForErrorHandling(args, Main.NO_ARGUMENTS_FOUND, "parseArguments() should've thrown an exception when argument provided is null");
  }

  @Test
  public void testWhenNoDividerInArg() {
    final String[] args = {"apex /some/path"};

    testParseArgForErrorHandling(args, String.format(Main.EXPECTED_DIVIDER, args[0]), "parseArguments() should've thrown an exception when argument doesn't have a " + Main.DIVIDER);
  }

  @Test
  public void testWhenAPartOfArgIsMissing() {
    final String[] args = {"=/some/path"};
    final String expectedArgForMessage = String.format(Main.MISSING_PARTS, args[0]);
    final String failureMessage = "parseArguments() should've thrown an exception when argument is missing a part of the string";

    testParseArgForErrorHandling(args, expectedArgForMessage, failureMessage);
  }

  @Test
  public void testWhenPathIsWeirdlyEmpty() {
    final String language = "apex";
    final String[] args = {language + "=,,,"};
    final String expectedArgForMessage = String.format(Main.NO_PATH_PROVIDED, language, args[0]);
    final String failureMessage = "parseArguments() should've thrown an exception when path is effectively empty";

    testParseArgForErrorHandling(args, expectedArgForMessage, failureMessage);
  }


  private void testParseArgForErrorHandling(String[] args, String expectedArgForMessage, String failureMessage) {
    try {
      main.parseArguments(args);
      fail(failureMessage);
    } catch (SfdxScannerException e) {
      assertEquals("Unexpected eventKey on exception", EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, e.getEventKey());
      assertEquals("Unexpected arg list on exception", expectedArgForMessage, e.getArgs()[0]);
    }
  }
}

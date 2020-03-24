package sfdc.sfdx.scanner.pmd;

import java.util.*;
import java.util.stream.Collectors;

import sfdc.sfdx.scanner.EventKey;
import sfdc.sfdx.scanner.SfdxScannerException;
import sfdc.sfdx.scanner.messaging.SfdxMessager;

public class Main {

  static String DIVIDER = "=";
  static String COMMA = ",";

  static final String NO_ARGUMENTS_FOUND = "No arguments found";
  static final String EXPECTED_DIVIDER = "Expected one " + DIVIDER + " in argument: %s";
  static final String MISSING_PARTS = "Missing language and/or paths in argument: %s";
  static final String NO_PATH_PROVIDED = "At least one path needs to be provided for language %s in argument: %s";

  Dependencies dependencies;
  public Main() {
    this(new Dependencies());
  }

  public Main(Dependencies dependencies) {
    this.dependencies = dependencies;
  }

  /**
   * Main entry to PMD Rule Catalog builder
   * @param args should contain language separated by '=' from their comma-separated path mapping list. For example, here are some accepted arg values :
   *             "apex=/some/lib/withjars,/more/path"
   *             "javascript=/another/path,/yet/another/path/with/javascript/rules"
   */
  public static void main(String[] args) {

    final Main main = new Main(new Dependencies());
    main.mainInternal(args);
  }

  void mainInternal(String[] args) {
    try {
      final Map<String, List<String>> rulePathEntries = parseArguments(args);
      catalogRules(rulePathEntries);

    } catch (SfdxScannerException se) {
      // Add all SfdxScannerExceptions as messages
      SfdxMessager.getInstance().uxError(se);
    } catch (Throwable throwable) {
      // Catch and handle any exceptions that may have slipped through
      final SfdxScannerException exception = new SfdxScannerException(EventKey.ERROR_UNEXPECTED, new String[]{throwable.getMessage()}, throwable);
      SfdxMessager.getInstance().uxError(exception);
    }
    finally {
      // Print all the messages we have collected in a parsable format
      System.out.println(SfdxMessager.getInstance().getAllMessagesWithFormatting());
    }
  }

  private void catalogRules(Map<String, List<String>> rulePathEntries) {
    PmdRuleCataloger prc = dependencies.getPmdRuleCataloger(rulePathEntries);
    prc.catalogRules();
  }

  Map<String, List<String>> parseArguments(String[] args) {
    if (args == null || args.length < 1) {
      throw new SfdxScannerException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, new String[]{NO_ARGUMENTS_FOUND});
    }

    final Map<String, List<String>> rulePathEntries = new HashMap<>();

    for (String arg : args) {
      parseArg(rulePathEntries, arg);
    }
    return rulePathEntries;
  }

  private void parseArg(Map<String, List<String>> languagePathEntries, String arg) {
    final String[] splitArg = arg.split(DIVIDER);

    // DIVIDER should split arg in language and path list. No less, no more
    if (splitArg.length != 2) {
      throw new SfdxScannerException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, new String[]{String.format(EXPECTED_DIVIDER, arg)});
    }
    final String language = splitArg[0].trim();
    final String paths = splitArg[1];

    if ("".equals(language.trim()) || "".equals((paths.trim()))) {
      throw new SfdxScannerException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, new String[]{String.format(MISSING_PARTS, arg)});
    }

    final String[] pathArray = paths.split(COMMA);

    if (pathArray.length < 1) {
      throw new SfdxScannerException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, new String[]{String.format(NO_PATH_PROVIDED, language, arg)});
    }

    // Stream path array to filter out empty path
    List<String> pathList = Arrays.stream(pathArray).filter(path -> !"".equals(path.trim())).collect(Collectors.toList());

    languagePathEntries.put(language, pathList);
  }

  static class Dependencies {

    PmdRuleCataloger getPmdRuleCataloger(Map<String, List<String>> rulePathEntries) {
      return new PmdRuleCataloger(rulePathEntries);
    }
  }
}

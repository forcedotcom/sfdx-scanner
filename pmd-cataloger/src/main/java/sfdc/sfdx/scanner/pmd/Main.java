package sfdc.sfdx.scanner.pmd;

import java.util.*;
import java.util.stream.Collectors;

import sfdc.sfdx.scanner.ExitCode;

public class Main {

  private static String DIVIDER = "=";
  private static String COMMA = ",";
  /**
   * Main entry to PMD Rule Catalog builder
   * @param args should contain language separated by '=' from their comma-separated path mapping list. For example, here are some accepted arg values :
   *             "apex=/some/lib/withjars,/more/path"
   *             "javascript=/another/path,/yet/another/path/with/javascript/rules"
   */
  public static void main(String[] args) {

    if (args.length < 1) {
      System.err.println("No arguments found. Please provide language to list of path mapping for each language to support");
      System.exit(ExitCode.MAIN_INVALID_ARGUMENT.getCode());
    }

    final Map<String, List<String>> rulePathEntries = new HashMap<>();

    for (String arg: args) {
      parseArg(rulePathEntries, arg);
    }


    PmdRuleCataloger prc = new PmdRuleCataloger(rulePathEntries);
    prc.catalogRules();
  }

  private static void parseArg(Map<String, List<String>> languagePathEntries, String arg) {
    final String[] splitArg = arg.split(DIVIDER);

    // DIVIDER should split arg in language and path list. No less, no more
    if (splitArg.length != 2) {
      System.err.println("Expected one " + DIVIDER + " in argument: " + arg);
      System.exit(ExitCode.MAIN_INVALID_ARGUMENT.getCode());
    }
    final String language = splitArg[0].trim();
    final String paths = splitArg[1];

    if ("".equals(language.trim()) || "".equals((paths.trim()))) {
      System.err.println("Missing language and/or paths in argument: " + arg);
      System.exit(ExitCode.MAIN_INVALID_ARGUMENT.getCode());
    }

    final String[] pathArray = paths.split(COMMA);

    if (pathArray.length < 1) {
      System.err.println("AT least one path needs to be provided for language " + language + " in argument: " + arg);
      System.exit(ExitCode.MAIN_INVALID_ARGUMENT.getCode());
    }

    // Stream path array to filter out empty path
    List<String> pathList = Arrays.stream(pathArray).filter(path -> !"".equals(path.trim())).collect(Collectors.toList());

    languagePathEntries.put(language, pathList);
  }

}

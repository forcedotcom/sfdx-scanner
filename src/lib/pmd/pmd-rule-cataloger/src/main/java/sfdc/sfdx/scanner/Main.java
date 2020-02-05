package sfdc.sfdx.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // We need there to be exactly three arguments, so throw an error if we didn't get them.
    if (args.length != 3) {
      // It's probably fine for this error message to be hardcoded, because it should never ever ever be seen by anyone
      // outside of the ISV SWAT team.
      System.err.println("Catalog build command received " + args.length + " argument(s) instead of the required three.");
      System.exit(ExitCode.WRONG_ARG_COUNT.getCode());
    }
    String pmdPath = args[0];
    String pmdVersion = args[1];
    List<String> supportedLangs = new ArrayList<>(Arrays.asList(args[2].split(",")));

    PmdRuleCataloger prc = new PmdRuleCataloger(pmdVersion, pmdPath, supportedLangs);
    prc.catalogRules();
  }
}

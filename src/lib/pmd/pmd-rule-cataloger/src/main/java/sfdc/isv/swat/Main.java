package sfdc.isv.swat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // We need there to be exactly three arguments, so throw an error if we didn't get them.
    if (args.length != 3) {
      // TODO: IMPROVE ERROR HANDLING HERE.
      System.out.println("We need three arguments. Instead we got " + args.length);
      System.exit(1);
    }
    String pmdPath = args[0];
    String pmdVersion = args[1];
    List<String> supportedLangs = new ArrayList<>(Arrays.asList(args[2].split(",")));

    PmdRuleCataloger prc = new PmdRuleCataloger(pmdVersion, pmdPath, supportedLangs);
    prc.catalogRules();
  }
}

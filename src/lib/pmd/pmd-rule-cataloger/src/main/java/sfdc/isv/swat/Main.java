package sfdc.isv.swat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
  public static void main(String[] args) {




    // TODO: We want the set of languages to be configurable in some way. So this should be dynamically populated either
    //  through arguments or by reading a file.
    List<String> supportedLangs = new ArrayList<>(Arrays.asList("javascript", "apex"));

    PmdRuleCataloger prc = new PmdRuleCataloger("6.20.0", "./dist/pmd/lib", supportedLangs);
    prc.catalogRules();
  }
}

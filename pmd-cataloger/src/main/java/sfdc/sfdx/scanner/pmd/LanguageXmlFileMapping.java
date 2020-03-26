package sfdc.sfdx.scanner.pmd;

import sfdc.sfdx.scanner.messaging.EventKey;
import sfdc.sfdx.scanner.messaging.SfdxMessager;

import java.util.*;

/**
 * Maintains mapping between languages and their XML file paths
 */
public class LanguageXmlFileMapping {
  private final static String CATEGORY = "category";
  private final static String RULESETS = "rulesets";

  private static LanguageXmlFileMapping INSTANCE = new LanguageXmlFileMapping();

  private Map<String, Set<String>> categoryPathsByLanguage;
  private Map<String, Set<String>> rulesetPathsByLanguage;

  private LanguageXmlFileMapping() {
    categoryPathsByLanguage = new HashMap<>();
    rulesetPathsByLanguage = new HashMap<>();
  }

  public static LanguageXmlFileMapping getInstance() {
    return INSTANCE;
  }

  public void addPathsForLanguage(List<String> paths, String language) {
    paths.forEach(path -> addPathForLanguage(path, language));
  }

  public Map<String, Set<String>> getCategoryPaths() {
    return this.categoryPathsByLanguage;
  }

  public Map<String, Set<String>> getRulesetPaths() {
    return this.rulesetPathsByLanguage;
  }

  // We want to distinguish XMLs as a Ruleset or a Category.
  // If path looks like */rulesets/**/*.xml, we consider the XML a Ruleset
  // If path looks like */category/**/*.xml, we consider the XML a Category
  private void addPathForLanguage(String path, String language) {
    if (! nullEmptyOrWhitespace(path)) {
      if (path.contains(RULESETS)) {
        addRulesetPathForLanguage(path, language);
      } else if (path.contains(CATEGORY)) {
        addCategoryPathForLanguage(path, language);
      } else {
        SfdxMessager.getInstance().addMessage("Adding path " + path + " for language " + language, EventKey.WARNING_XML_DROPPED, path);
      }
    }
  }

  private void addRulesetPathForLanguage(String path, String language) {
    addPath(path, language, rulesetPathsByLanguage);
  }

  private void addCategoryPathForLanguage(String path, String language) {
    addPath(path, language, categoryPathsByLanguage);
  }

  private void addPath(String path, String language, Map<String, Set<String>> pathsByLanguage) {
    language = language.toLowerCase();

    if (pathsByLanguage.containsKey(language)) {
      pathsByLanguage.get(language).add(path);
    } else {
      final Set<String> values = new HashSet<>();
      values.add(path);
      pathsByLanguage.put(language, values);
    }
  }

  boolean nullEmptyOrWhitespace (String someString) {
    return !((someString != null) && (someString.trim().length() > 0));
  }

  @Override
  public String toString() {
    return "LanguageXmlFileMapping{" +
      "categoryPathsByLanguage=" + categoryPathsByLanguage +
      ", rulesetPathsByLanguage=" + rulesetPathsByLanguage +
      '}';
  }
}

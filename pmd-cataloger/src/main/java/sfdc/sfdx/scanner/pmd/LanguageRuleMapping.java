package sfdc.sfdx.scanner.pmd;

import java.util.*;

/**
 * Maintains mapping between languages and their XML paths
 */
public class LanguageRuleMapping {
  private final static String CATEGORY = "category";
  private final static String RULESETS = "rulesets";

  private static LanguageRuleMapping INSTANCE = new LanguageRuleMapping();

  private Map<String, Set<String>> categoryPathsByLanguage;
  private Map<String, Set<String>> rulesetPathsByLanguage;

  private LanguageRuleMapping() {
    categoryPathsByLanguage = new HashMap<>();
    rulesetPathsByLanguage = new HashMap<>();
  }

  public static LanguageRuleMapping getInstance() {
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

  void addPathForLanguage(String path, String language) {
    if (! nullEmptyOrWhitespace(path)) {
      if (path.contains(RULESETS)) {
        addPathForLanguageToRulesets(path, language);
      } else if (path.contains(CATEGORY)) {
        addPathForLanguageToCategory(path, language);
      } else {
        // TODO: temporarily default to Category. But this won't work in all cases.
        addPathForLanguageToCategory(path, language);

      }
    }
  }

  private void addPathForLanguageToRulesets(String path, String language) {
    addPath(path, language, rulesetPathsByLanguage);
  }

  private void addPathForLanguageToCategory(String path, String language) {
    addPath(path, language, categoryPathsByLanguage);
  }

  private void addPath(String path, String language, Map<String, Set<String>> pathsByLanguage) {
    if (nullEmptyOrWhitespace(language)) {
      throw new ScannerPmdException("Language cannot be empty: " + language);
    }

    final String lowerLanguage = language.toLowerCase();

    if (pathsByLanguage.containsKey(lowerLanguage)) {
      pathsByLanguage.get(lowerLanguage).add(path);
    } else {
      final Set<String> values = new HashSet<>();
      values.add(path);
      pathsByLanguage.put(lowerLanguage, values);
    }
  }

  boolean nullEmptyOrWhitespace (String someString) {
    return !((someString != null) && (someString.trim().length() > 0));
  }

  @Override
  public String toString() {
    return "LanguageRuleMapping{" +
      "categoryPathsByLanguage=" + categoryPathsByLanguage +
      ", rulesetPathsByLanguage=" + rulesetPathsByLanguage +
      '}';
  }
}

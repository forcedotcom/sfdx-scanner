package sfdc.sfdx.scanner.pmd;

import sfdc.sfdx.scanner.messaging.EventKey;
import sfdc.sfdx.scanner.messaging.SfdxMessager;
import sfdc.sfdx.scanner.messaging.SfdxScannerException;

import java.util.*;

/**
 * Maintains mapping between languages and their XML file paths
 */
public class LanguageXmlFileMapping {
	private final static String CATEGORY = "category";
	private final static String RULESETS = "rulesets";

	private Map<String, Set<String>> categoryPathsByLanguage;
	private Map<String, Set<String>> rulesetPathsByLanguage;
	private Map<String, String> categoryToSourceJar;
	private Map<String, String> rulesetToSourceJar;

	public LanguageXmlFileMapping() {
		categoryPathsByLanguage = new HashMap<>();
		rulesetPathsByLanguage = new HashMap<>();
		categoryToSourceJar = new HashMap<>();
		rulesetToSourceJar = new HashMap<>();
	}

	public void addPathsForLanguage(List<XmlFileFinder.XmlContainer> xmlContainers, String language) {
		xmlContainers.forEach(xmlContainer -> {
			xmlContainer.containedFilePaths.forEach(containedFilePath -> {
				addPathForLanguage(containedFilePath, language, xmlContainer.filePath);
			});
		});
	}

	public Map<String, Set<String>> getCategoryPaths() {
		return this.categoryPathsByLanguage;
	}

	public Map<String, Set<String>> getRulesetPaths() {
		return this.rulesetPathsByLanguage;
	}

	public String getSourceJarForCategory(String catPath) {
		return this.categoryToSourceJar.get(catPath);
	}

	// We want to distinguish XMLs as a Ruleset or a Category.
	// If path looks like */rulesets/**/*.xml, we consider the XML a Ruleset
	// If path looks like */category/**/*.xml, we consider the XML a Category
	private void addPathForLanguage(String path, String language, String sourceJar) {
		if (!nullEmptyOrWhitespace(path)) {
			if (path.contains(RULESETS)) {
				addRulesetPathForLanguage(path, language, sourceJar);
				rulesetToSourceJar.put(path, sourceJar);
			} else if (path.contains(CATEGORY)) {
				addCategoryPathForLanguage(path, language, sourceJar);
				categoryToSourceJar.put(path, sourceJar);
			} else {
				SfdxMessager.getInstance().addMessage("Adding path " + path + " for language " + language, EventKey.WARNING_XML_DROPPED, path);
			}
		}
	}

	private void addRulesetPathForLanguage(String path, String language, String sourceJar) {
		addPath(path, language, sourceJar, rulesetPathsByLanguage);
	}

	private void addCategoryPathForLanguage(String path, String language, String sourceJar) {
		addPath(path, language, sourceJar, categoryPathsByLanguage);
	}

	private void addPath(String path, String language, String sourceJar, Map<String, Set<String>> pathsByLanguage) {
		language = language.toLowerCase();

		String jarWithConflict = null;
		if (rulesetToSourceJar.containsKey(path)) {
			jarWithConflict = rulesetToSourceJar.get(path);
		} else if (categoryToSourceJar.containsKey(path)) {
			jarWithConflict = categoryToSourceJar.get(path);
		}

		if (jarWithConflict != null) {
			throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_DUPLICATE_XML_PATH, path, sourceJar, jarWithConflict);
		}

		if (pathsByLanguage.containsKey(language)) {
			pathsByLanguage.get(language).add(path);
		} else {
			final Set<String> values = new HashSet<>();
			values.add(path);
			pathsByLanguage.put(language, values);
		}
	}

	boolean nullEmptyOrWhitespace(String someString) {
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

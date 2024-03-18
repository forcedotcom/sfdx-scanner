package sfdc.sfdx.scanner.pmd;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.w3c.dom.*;
import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.MessagePassableException;
import com.salesforce.messaging.CliMessager;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogCategory;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogJson;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogRule;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogRuleset;
import sfdc.sfdx.scanner.xml.XmlReader;
import sfdc.sfdx.scanner.paths.PathManipulator;

class PmdRuleCataloger {
	private final Map<String, List<String>> rulePathEntries;

	// Holds category and rulesets maps that provide files we need to scan for each language.
	private final LanguageXmlFileMapping languageXmlFileMapping = new LanguageXmlFileMapping();

	// These maps are going to help us store intermediate objects in an easy-to-reference way.
	private final Map<String, List<PmdCatalogRule>> rulesByLanguage = new HashMap<>();
	private final Map<String, List<PmdCatalogRuleset>> rulesetsByLanguage = new HashMap<>();

	// These lists are going to be the master lists that we ultimately use to build our JSON at the end.
	private final List<PmdCatalogCategory> masterCategoryList = new ArrayList<>();
	private final List<PmdCatalogRule> masterRuleList = new ArrayList<>();
	private final List<PmdCatalogRuleset> masterRulesetList = new ArrayList<>();

    /**
     * The directory in which the catalog file will be placed.
     */
    private final String catalogHome;
    /**
     * The name that the catalog file will be given.
     */
    private final String catalogName;
    /**
     * The specific PMD variant whose rules are being cataloged. (E.g., "pmd" vs "pmd-appexchange")
     */
    private final String engineSubvariant;


	/**
	 * @param rulePathEntries Map of languages and their file resources. Includes both inbuilt PMD and custom rules provided by user
	 */
	PmdRuleCataloger(Map<String, List<String>> rulePathEntries, String catalogHome, String catalogName, String engineSubvariant) {
		this.rulePathEntries = rulePathEntries;
        this.catalogHome = catalogHome;
        this.catalogName = catalogName;
        this.engineSubvariant = engineSubvariant;
	}


	/**
	 * Builds a catalog describing all of the rules, rulesets, and categories defined by PMD for the used languages, and
	 * stores them in a JSON file.
	 */
	void catalogRules() {
		//Check for custom rules and process them

		// Identify all the ruleset and category files for each language we're looking at.
		extractRules();

		// STEP: Process the category files to derive category and rule representations.
		final Map<String, Set<String>> categoryPathsByLanguage = this.languageXmlFileMapping.getCategoryPaths();
		for (String language : categoryPathsByLanguage.keySet()) {
			final Set<String> categoryPaths = categoryPathsByLanguage.get(language);
			for (String categoryPath : categoryPaths) {
				processCategoryFile(language, categoryPath);
			}
		}

		// Process the ruleset files.
		final Map<String, Set<String>> rulesetPathsByLanguage = this.languageXmlFileMapping.getRulesetPaths();
		for (String language : rulesetPathsByLanguage.keySet()) {
			Set<String> rulesetPaths = rulesetPathsByLanguage.get(language);
			// For each ruleset, generate a representation.
			for (String rulesetPath : rulesetPaths) {
				generateRulesetRepresentation(language, rulesetPath);
			}
			// Create links between dependent rulesets.
			linkDependentRulesets(rulesetsByLanguage.get(language));
		}

		// Link rules to the rulesets that reference them.
		for (String language : rulesetsByLanguage.keySet()) {
			List<PmdCatalogRuleset> rulesets = rulesetsByLanguage.get(language);
			List<PmdCatalogRule> rules = rulesByLanguage.get(language);
			linkRulesToRulesets(rules, rulesets);
		}

		// Build a JSON using all of our objects.
		PmdCatalogJson json = new PmdCatalogJson(masterRuleList, masterCategoryList, masterRulesetList, engineSubvariant);

		// Write the JSON to a file.
		writeJsonToFile(json);
	}


	void extractRules() {
		// Find XML files for each language in each resource path
		final XmlFileFinder xmlFileFinder = new XmlFileFinder();

		rulePathEntries.keySet().forEach(language -> {
			List<String> filePaths = rulePathEntries.get(language);
			filePaths.forEach(filePath -> {
				List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder.findXmlFilesInPath(filePath);
				languageXmlFileMapping.addPathsForLanguage(xmlContainers, language);
			});
		});

	}

	private void processCategoryFile(String language, String path) {
		// STEP 1: Turn the category file's XML into a Document object with a Root Element that we can actually use.
		Document doc = XmlReader.getInstance().getDocumentFromPath(path);
		Element root = doc.getDocumentElement();
		// If the root node isn't of type 'ruleset', this isn't a valid category file, so we should just log something and skip it.
		if (!root.getTagName().equalsIgnoreCase("ruleset") || !root.getAttribute("xmlns").startsWith("http://pmd.sourceforge.net")) {
			String fullPath = PathManipulator.getInstance().convertResourcePathToAbsolutePath(path);
			CliMessager.getInstance().addMessage("Processing category file for language " + language + " at path " + path, EventKey.WARNING_INVALID_CAT_SKIPPED, fullPath);
			return;
		}

		// STEP 2: Use the root element to derive a Category representation, and put it in the master list.
		String categoryName = root.getAttribute("name");
		PmdCatalogCategory category = new PmdCatalogCategory(categoryName, path, languageXmlFileMapping.getSourceJarForCategory(path));
		this.masterCategoryList.add(category);

		// STEP 3: Get the "rule"-type nodes and use them to create Rule representations, which we should map to the target
		// language and also put in the master list.
		NodeList ruleNodes = root.getElementsByTagName("rule");
		List<PmdCatalogRule> rules = new ArrayList<>();
		int ruleCount = ruleNodes.getLength();
		for (int i = 0; i < ruleCount; i++) {
			Element ruleNode = (Element) ruleNodes.item(i);
			PmdCatalogRule rule = new PmdCatalogRule(ruleNode, category, language, engineSubvariant);
			rules.add(rule);
		}
		if (!this.rulesByLanguage.containsKey(language)) {
			this.rulesByLanguage.put(language, new ArrayList<>());
		}
		this.rulesByLanguage.get(language).addAll(rules);
		this.masterRuleList.addAll(rules);
	}

	private void generateRulesetRepresentation(String language, String path) {
		// STEP 1: Turn the ruleset file's XML into a Document object with a Root Element that we can actually use.
		Document doc = XmlReader.getInstance().getDocumentFromPath(path);
		Element root = doc.getDocumentElement();
		// If the root node isn't of type 'ruleset', this isn't a valid ruleset file, so we should just log something and skip it.
		if (!root.getTagName().equalsIgnoreCase("ruleset") || !root.getAttribute("xmlns").startsWith("http://pmd.sourceforge.net")) {
			String fullPath = PathManipulator.getInstance().convertResourcePathToAbsolutePath(path);
			CliMessager.getInstance().addMessage("Generating Ruleset representation for language " + language + " at path " + path, EventKey.WARNING_INVALID_RULESET_SKIPPED, fullPath);
			return;
		}

		// STEP 2: Use the root element to derive a Ruleset representation, which we should map to the target language and
		// also put in the master list.
		PmdCatalogRuleset ruleset = new PmdCatalogRuleset(root, path);
		if (!this.rulesetsByLanguage.containsKey(language)) {
			this.rulesetsByLanguage.put(language, new ArrayList<>());
		}
		this.rulesetsByLanguage.get(language).add(ruleset);
		masterRulesetList.add(ruleset);
	}

	private void linkDependentRulesets(List<PmdCatalogRuleset> rulesets) {
		// Map the rulesets by their path.
		Map<String, PmdCatalogRuleset> rulesetsByPath = new HashMap<>();
		for (PmdCatalogRuleset ruleset : rulesets) {
			rulesetsByPath.put(ruleset.getPath(), ruleset);
		}

		// Next, feed the map into each ruleset to see which ones it depends on.
		for (PmdCatalogRuleset ruleset : rulesets) {
			ruleset.processDependencies(rulesetsByPath);
		}
	}

	private void linkRulesToRulesets(List<PmdCatalogRule> rules, List<PmdCatalogRuleset> rulesets) {
		for (PmdCatalogRule rule : rules) {
			for (PmdCatalogRuleset ruleset : rulesets) {
				ruleset.processRule(rule);
			}
		}
	}

	private void writeJsonToFile(PmdCatalogJson json) {
		CliMessager.getInstance().addMessage(String.format("Received catalogHome as %s and catalogName as %s", catalogHome, catalogName), EventKey.INFO_GENERAL_INTERNAL_LOG, "PmdRuleCataloger.writeJsonToFile()");
		Path catDirPath = Paths.get(catalogHome);
		Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString = prettyGson.toJson(json.constructJson());
		persistJsonToFile(jsonString, catDirPath, catalogName);
	}

	void persistJsonToFile(String contents, Path directoryPath, String fileName) {
		File directoryFile = directoryPath.toFile();
		directoryFile.mkdir();
		try (
			FileWriter fileWriter = new FileWriter(Paths.get(directoryPath.toString(), fileName).toString())
		) {
			fileWriter.write(contents);
		} catch (IOException ioe) {
			throw new MessagePassableException(EventKey.ERROR_INTERNAL_JSON_WRITE_FAILED, ioe, fileName);
		}
	}
}

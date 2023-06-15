package sfdc.sfdx.scanner.pmd.catalog;

import java.util.*;

import org.w3c.dom.*;
import org.json.simple.*;
import com.salesforce.messaging.MessagePassableException;
import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.CliMessager;

import static sfdc.sfdx.scanner.pmd.catalog.PmdCatalogJson.*;

public class PmdCatalogRule {
	public static final String ATTR_NAME = "name";
	public static final String ATTR_MESSAGE = "message";
	public static final String ATTR_DESCRIPTION = "description";

    public static final String ATTR_LANGUAGE = "language";
    public static final String ATTR_DEPRECATED = "deprecated";

	private final String name;
	private final String message;
	private final String description;
	private final String language;
	private final String sourceJar;
	/**
	 * Seemingly all rules are defined in category XML files, so we can reasonably assume that each rule is a member of only
	 * one category.
	 */
	private final PmdCatalogCategory category;
	/**
	 * Rules can be included in an arbitrary number of rulesets.
	 */
	private final Set<PmdCatalogRuleset> rulesets = new HashSet<>();


	public PmdCatalogRule(Element element, PmdCatalogCategory category, String language) {
		this.name = element.getAttribute(ATTR_NAME);
		this.message = element.getAttribute(ATTR_MESSAGE);
		this.language = language;
		this.category = category;
		this.description = getDescription(element);
		this.sourceJar = category.getSourceJar();
        validatePmd7Readiness(element);
	}

	String getFullName() {
		return getCategoryPath() + "/" + getName();
	}

	String getName() {
		return name;
	}

	String getCategoryPath() {
		return category.getPath();
	}

	/**
	 * Adds the provided ruleset to the list of rulesets of which this rule is a member.
	 *
	 * @param ruleset - A ruleset of which this rule should be a member.
	 */
	void addRuleset(PmdCatalogRuleset ruleset) {
		rulesets.add(ruleset);
	}

	/**
	 * @param element - A 'rule'-type node.
	 * @return - A (possibly multi-line) string pulled from the input node's description-type child.
	 */
	private String getDescription(Element element) {
		// The rule node should have at most one "description" node, so get that.
		NodeList nl = element.getElementsByTagName(ATTR_DESCRIPTION);
		String res;
		switch (nl.getLength()) {
			case 0:
				// Technically there should always be a description node, but if there wasn't one, we'll just return an empty string.
				res = "";
				break;
			case 1:
				Element descriptionNode = (Element) nl.item(0);
				res = descriptionNode.getTextContent();
				break;
			default:
				// If there was more than one description node, then something's gone crazy wrong and we should exit as gracefully
				// as possible.
				throw new MessagePassableException(EventKey.ERROR_EXTERNAL_MULTIPLE_RULE_DESC, getFullName(), String.valueOf(nl.getLength()));
		}
		return res;
	}

    private void validatePmd7Readiness(Element element) {
        // PMD 7 expects rules to have a "language" property.
        String langProp = element.getAttribute(ATTR_LANGUAGE);
        // Rules can be deprecated.
        boolean deprecated = element.getAttribute(ATTR_DEPRECATED).equalsIgnoreCase("true");

        // If the rule lacks the necessary property and isn't deprecated, log a warning for it.
        if (langProp.isEmpty() && !deprecated) {
            CliMessager.getInstance().addMessage("Rule " + name + " is incompatible with PMD 7", EventKey.WARNING_PMD7_INCOMPATIBLE_RULE, name);
        }
    }

	/**
	 * Converts this rule into a JSONObject.
	 *
	 * @return - A JSONObject representing this rule.
	 */
	JSONObject toJson() {
		Map<String, Object> m = new HashMap<>();
		m.put(JSON_ENGINE, PMD_ENGINE_NAME);
		m.put(JSON_NAME, this.name);
		m.put(JSON_MESSAGE, this.message);
		m.put(JSON_DESCRIPTION, this.description);
		m.put(JSON_SOURCEPACKAGE, this.sourceJar);
		m.put(JSON_DEFAULTENABLED, true);
        m.put(JSON_ISDFA, false);
        m.put(JSON_ISPILOT, false);

		// We want 'languages' to be represented as an array even though PMD rules only run against one language, because
		// this way it's easier to integrate with the language-agnostic framework that we ultimately want.
		List<String> langs = new ArrayList<>(Collections.singletonList((this.language)));
		m.put(JSON_LANGUAGES, langs);

		// We also want 'categories' to be an array for the same reason.
		List<String> categoryNames = new ArrayList<>(Collections.singletonList(this.category.getName()));
		m.put(JSON_CATEGORIES, categoryNames);

		List<String> rulesetNames = new ArrayList<>();
		for (PmdCatalogRuleset ruleset : rulesets) {
			rulesetNames.add(ruleset.getName());
		}
		m.put(JSON_RULESETS, rulesetNames);

		return new JSONObject(m);
	}
}

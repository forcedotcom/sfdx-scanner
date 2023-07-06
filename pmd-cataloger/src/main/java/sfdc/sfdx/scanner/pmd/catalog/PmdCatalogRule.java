package sfdc.sfdx.scanner.pmd.catalog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import org.w3c.dom.*;
import org.json.simple.*;
import com.salesforce.messaging.MessagePassableException;
import com.salesforce.messaging.EventKey;

import static sfdc.sfdx.scanner.pmd.catalog.PmdCatalogJson.*;

public class PmdCatalogRule {
	public static final String ATTR_NAME = "name";
	public static final String ATTR_MESSAGE = "message";
	public static final String ATTR_DESCRIPTION = "description";

    public static final String ATTR_LANGUAGE = "language";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_REF = "ref";
    public static final String ELEM_PROPERTIES = "properties";
    public static final String ELEM_PROPERTY = "property";
    public static final String VAL_XPATH = "xpath";

    /**
     * Regex that matches the naming convention used by PMD's JARs.
     * Ordinarily, this would be less hardcoded, but PMD's not releasing new versions of 6,
     * so we can count on the JAR remaining constant until PMD 7 releases, at which point this
     * code will probably be obsolete.
     */
    private static final Pattern STANDARD_JAR_PATTERN = Pattern.compile("pmd-(apex|java|javascript|visualforce|xml)-6\\.55\\.0\\.jar", Pattern.CASE_INSENSITIVE);
    /**
     * Path used for comparison to identify standard rules.
     * See note on {@link #STANDARD_JAR_PATTERN} for information about why this is hardcoded.
     */
    private static final Path STANDARD_JAR_PATH = Paths.get("dist", "pmd", "lib");

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

    private final Element element;


	public PmdCatalogRule(Element element, PmdCatalogCategory category, String language) {
        this.element = element;
		this.name = element.getAttribute(ATTR_NAME);
		this.message = element.getAttribute(ATTR_MESSAGE);
		this.language = language;
		this.category = category;
		this.description = getDescription(element);
		this.sourceJar = category.getSourceJar();
	}

    public String getLanguage() {
        return language;
    }

	String getFullName() {
		return getCategoryPath() + "/" + getName();
	}

	public String getName() {
		return name;
	}

    public boolean isXpath() {
        NodeList properties = element.getElementsByTagName(ELEM_PROPERTIES);
        if (properties.getLength() == 0) {
            return false;
        }

        Element propertiesElem = (Element) properties.item(0);
        NodeList propertyList = propertiesElem.getElementsByTagName(ELEM_PROPERTY);
        for (int i = 0; i < propertyList.getLength(); i++) {
            Element elem = (Element) propertyList.item(i);
            if (elem.hasAttribute(ATTR_NAME) && elem.getAttribute(ATTR_NAME).equalsIgnoreCase(VAL_XPATH)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStandard() {
        // Make sure that we're in a JAR whose name matches PMD's naming convention.
        if (!STANDARD_JAR_PATTERN.matcher(this.sourceJar).find()) {
            return false;
        }
        // Make sure that we're in a directory that could plausibly be our dist directory.
        Path parentDir = Paths.get(this.sourceJar).getParent();
        return parentDir.endsWith(STANDARD_JAR_PATH);
    }

	String getCategoryPath() {
		return category.getPath();
	}

    public Element getElement() {
        return element;
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

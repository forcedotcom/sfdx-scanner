package sfdc.sfdx.scanner.pmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.Message;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogCategory;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogRule;

public class Pmd7CompatibilityCheckerTest {

    private static final String STANDARD_JAR = "/Users/me/sfdx-scanner/dist/pmd/lib/pmd-apex-6.55.0.jar";
    private static final String NONSTANDARD_JAR = "/Users/me/some/path/to/MyRules.jar";
    private static final String STANDARD_CATPATH = "category/apex/bestpractices.xml";
    private static final String NONSTANDARD_CATPATH = "category/apex/somewildcat.xml";

    /**
     * Before and after each test, reset the CLI messages.
     */
    @BeforeEach
    @AfterEach
    public void clearMessages() {
        CliMessager.getInstance().resetMessages();
    }

    @Test
    public void testStandardRules_expectCompatible() {
        // Create a category that purports to reside in a standard JAR.
        PmdCatalogCategory standardCategory = mockStandardCategory();

        // Use that category to create some rules that would otherwise be incompatible.
        List<PmdCatalogRule> rules = Arrays.asList(
            // Create an XPath rule that uses the old class property.
            createXpathRule(standardCategory, "net.sourceforge.pmd.lang.apex.rule.ApexXPathRule", true),
            // Create a Java-based rule without a Language property.
            createJavaBasedRule(standardCategory, false)
        );

        // Run the rules through the verifier.
        Pmd7CompatibilityChecker checker = new Pmd7CompatibilityChecker();
        checker.validatePmd7Readiness(rules);

        // Verify that no messages were sent.
        List<Message> messages = getMessages();
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testRulesWithGoodValues_expectCompatible() {
        // Create a category that purports to reside in a nonstandard JAR.
        PmdCatalogCategory nonstandardCategory = mockNonstandardCategory();

        // Use that category to create some good custom rules.
        List<PmdCatalogRule> rules = Arrays.asList(
            createXpathRule(nonstandardCategory, "net.sourceforge.pmd.lang.rule.XPathRule", true),
            createXpathRule(nonstandardCategory, "net.sourceforge.pmd.lang.xml.rule.DomXPathRule", true),
            createJavaBasedRule(nonstandardCategory, true)
        );

        // Run the rules through the verifier.
        Pmd7CompatibilityChecker checker = new Pmd7CompatibilityChecker();
        checker.validatePmd7Readiness(rules);

        // Verify that no messages were sent.
        List<Message> messages = getMessages();
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testRulesWithoutLanguageProp_expectIncompatible() {
        // Create a category that purports to reside in a nonstandard JAR.
        PmdCatalogCategory nonstandardCategory = mockNonstandardCategory();

        // Use that category to create some rules that lack the Language property.
        List<PmdCatalogRule> rules = Arrays.asList(
            // Create an XPath rule.
            createXpathRule(nonstandardCategory, "net.sourceforge.pmd.lang.rule.XPathRule", false),
            createJavaBasedRule(nonstandardCategory, false)
        );

        // Run the rules through the verifier.
        Pmd7CompatibilityChecker checker = new Pmd7CompatibilityChecker();
        checker.validatePmd7Readiness(rules);

        // Verify that two messages were sent.
        List<Message> messages = getMessages();
        assertEquals(2, messages.size());
    }

    @Test
    public void testBadClassXPathRules_expectIncompatible() {
        // Create a category that purports to reside in a nonstandard JAR.
        PmdCatalogCategory nonstandardCategory = mockNonstandardCategory();

        // Use that category to create an XPath rule whose class is a bad value.
        List<PmdCatalogRule> rules = Collections.singletonList(createXpathRule(nonstandardCategory, "net.sourceforge.pmd.lang.apex.rule.ApexXPathRule", true));

        // Run the rule through the verifier.
        Pmd7CompatibilityChecker checker = new Pmd7CompatibilityChecker();
        checker.validatePmd7Readiness(rules);

        // Verify that one message was sent.
        List<Message> messages = getMessages();
        assertEquals(1, messages.size());
    }

    private PmdCatalogCategory mockStandardCategory() {
        return new PmdCatalogCategory("StandardCat", STANDARD_CATPATH, STANDARD_JAR);
    }

    public PmdCatalogCategory mockNonstandardCategory() {
        return new PmdCatalogCategory("NonstandardCat", NONSTANDARD_CATPATH, NONSTANDARD_JAR);
    }

    private PmdCatalogRule createJavaBasedRule(PmdCatalogCategory category, boolean hasLangProp) {
        // No inner properties are needed, but we can hardcode the class property.
        String classProp = "net.sourceforge.pmd.lang.apex.rule.codestyle.ClassNamingConventionsRule";
        String ruleXml = createRuleXml(classProp, hasLangProp, "");
        Element ruleElement = createRuleElement(ruleXml);
        return new PmdCatalogRule(ruleElement, category, "apex");
    }

    private PmdCatalogRule createXpathRule(PmdCatalogCategory category, String classProp, boolean hasLangProp) {
        String propertiesTags =
            "<properties>"
                + "<property name=\"version\" value=\"2.0\"/>"
                + "<property name=\"xpath\">"
                + "<value>"
                // Use a CDATA copied from an internal rule.
                + "<![CDATA["
                + "//IfBlockStatement/BlockStatement[@CurlyBrace= false()]"
                + "]]>"
                + "</value>"
                + "</property>"
                + "</properties>";
        String ruleXml = createRuleXml(classProp, hasLangProp, propertiesTags);
        Element ruleElement = createRuleElement(ruleXml);
        return new PmdCatalogRule(ruleElement, category, "apex");
    }

    private String createRuleXml(String classProp, boolean hasLangProp, String innerXml) {
        // The first tag is always the same.
        String header =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

        // The rule tag's properties are basically the same regardless of case.
        String ruleTagProps = "name=\"SomeRule\" class=\"" + classProp + "\" message=\"Test Message\"";
        // If the rule is expected to have a language property, add that.
        if (hasLangProp) {
            ruleTagProps += " language=\"apex\"";
        }

        header +=
            "<rule " + ruleTagProps + ">"
                + "<description>Filler text</description>"
                + "<priority>3</priority>"
                // Add whatever internals were given to us.
                + innerXml
            + "</rule>";
        return header;
    }

    private Element createRuleElement(String xml) {
        Element ruleElement = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));
            ruleElement = doc.getDocumentElement();
        } catch (Exception ex) {
            fail("Failed to parse rule xml");
        }
        return ruleElement;
    }

    private List<Message> getMessages() {
        final String messagesInJson = CliMessager.getInstance().getAllMessages();
        assertNotNull(messagesInJson);

        // Deserialize JSON to verify further
        final List<Message> messages = new Gson().fromJson(messagesInJson, new TypeToken<List<Message>>() {
        }.getType());
        assertNotNull(messages);
        return messages;
    }
}

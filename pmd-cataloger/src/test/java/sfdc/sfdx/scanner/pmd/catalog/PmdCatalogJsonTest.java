package sfdc.sfdx.scanner.pmd.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

public class PmdCatalogJsonTest {
    private static final String RULE_NAME = "rule name";
    private static final String RULE_PATH = "rule path";
    private static final String CATEGORY_NAME = "category name";
    private static final String CATEGORY_PATH = "category path";

    @Test
    public void testConstructJson() {

        final List<PmdCatalogRule> rules = new ArrayList<>();
        final List<PmdCatalogCategory> categories = new ArrayList<>();
        final List<PmdCatalogRuleset> rulesets = new ArrayList<>();
        rules.add(getPmdCatalogRuleMock());
        categories.add(getPmdCatalogCategoryMock(CATEGORY_NAME, CATEGORY_PATH));
        rulesets.add(getPmdCatalogRulesetMock(RULE_NAME, RULE_PATH));

        final PmdCatalogJson catalogJson = new PmdCatalogJson(rules, categories, rulesets);

        // Execute
        final JSONObject jsonObject = catalogJson.constructJson();

        // Verify
        //[{"paths":["rule path"],"name":"rule name"}]
        final String expectedRulesetJson = String.format("[{\"engine\":\"%s\",\"paths\":[\"%s\"],\"name\":\"%s\"}]",
            PmdCatalogJson.PMD_ENGINE_NAME, RULE_PATH, RULE_NAME);
        assertEquals(expectedRulesetJson, jsonObject.get(PmdCatalogJson.JSON_RULESETS).toString());

        final String expectedCategoryJson = String.format("[{\"engine\":\"%s\",\"paths\":[\"%s\"],\"name\":\"%s\"}]",
            PmdCatalogJson.PMD_ENGINE_NAME, CATEGORY_PATH, CATEGORY_NAME);
        assertEquals(expectedCategoryJson, jsonObject.get(PmdCatalogJson.JSON_CATEGORIES).toString());

        // Rules json has its own test where we verify the Json contents. Here, we only confirm that it exists
        assertTrue(jsonObject.containsKey(PmdCatalogJson.JSON_RULES), "JSON should contain 'rules' element");
    }

    private PmdCatalogRule getPmdCatalogRuleMock() {
        final PmdCatalogRule catalogRule = mock(PmdCatalogRule.class);
        final JSONObject jsonObject = mock(JSONObject.class);

        doReturn(jsonObject).when(catalogRule).toJson();

        return catalogRule;
    }

    private PmdCatalogCategory getPmdCatalogCategoryMock(String name, String path) {
        final PmdCatalogCategory category = mock(PmdCatalogCategory.class);

        doReturn(name).when(category).getName();
        doReturn(path).when(category).getPath();

        return category;
    }

    private PmdCatalogRuleset getPmdCatalogRulesetMock(String name, String path) {
        final PmdCatalogRuleset ruleset = mock(PmdCatalogRuleset.class);

        doReturn(name).when(ruleset).getName();
        doReturn(path).when(ruleset).getPath();

        return ruleset;
    }
}

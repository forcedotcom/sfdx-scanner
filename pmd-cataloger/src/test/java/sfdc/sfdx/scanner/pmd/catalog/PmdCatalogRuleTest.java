package sfdc.sfdx.scanner.pmd.catalog;

import org.json.simple.JSONObject;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;


public class PmdCatalogRuleTest {

  private static final String NAME = "Name";
  private static final String MESSAGE = "Some message";
  private static final String LANGUAGE = "apex";

  private static final String CATEGORY_NAME = "Best Practices";
  private static final String CATEGORY_PATH = "/some/path";
  private static final PmdCatalogCategory CATEGORY = new PmdCatalogCategory(CATEGORY_NAME, CATEGORY_PATH);

  @Test
  public void testCatalogRuleJsonConversion() {
    // Setup mock
    final Element elementMock = getElementMock(1, "description");
    final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);


    // Execute
    final JSONObject jsonObject = catalogRule.toJson();


    // Validate
    assertEquals("Unexpected name on JSON", NAME, jsonObject.get(PmdCatalogRule.JSON_NAME));

    assertEquals("Unexpected message", MESSAGE, jsonObject.get(PmdCatalogRule.JSON_MESSAGE));

    final List<String> expectedLanguages = new ArrayList<>();
    expectedLanguages.add(LANGUAGE);
    assertEquals("Unexpected language", expectedLanguages, (List<String>)jsonObject.get(PmdCatalogRule.JSON_LANGUAGES));

    final List<String> expectedCategoryNames = new ArrayList<>();
    expectedCategoryNames.add(CATEGORY_NAME);
    assertEquals("Unexpected categories", expectedCategoryNames, jsonObject.get(PmdCatalogRule.JSON_CATEGORIES));

  }

  @Test
  public void testCatalogRuleNoDescription() {

    final int descriptionNlCount = 0;
    final String emptyDescription = "";

    // Setup mock
    final Element elementMock = getElementMock(descriptionNlCount, emptyDescription);
    final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);

    // Execute
    final JSONObject jsonObject = catalogRule.toJson();

    // Validate
    assertEquals("Unexpected description", emptyDescription, jsonObject.get(PmdCatalogRule.JSON_DESCRIPTION));
  }

  @Test
  public void testCatalogRuleJsonWithDescription() {
    final int descriptionNlCount = 1;
    final String description = "Some description";

    // Setup mock
    final Element elementMock = getElementMock(descriptionNlCount, description);
    final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);

    // Execute
    final JSONObject jsonObject = catalogRule.toJson();

    // Validate
    assertEquals("Unexpected description", description, jsonObject.get(PmdCatalogRule.JSON_DESCRIPTION));
  }

  private Element getElementMock(int descriptionNlCount, String emptyDescription) {
    final Element elementMock = mock(Element.class);
    doReturn(NAME).when(elementMock).getAttribute(PmdCatalogRule.ATTR_NAME);
    doReturn(MESSAGE).when(elementMock).getAttribute(PmdCatalogRule.ATTR_MESSAGE);

    final Element descElementMock = mock(Element.class);
    doReturn(emptyDescription).when(descElementMock).getTextContent();

    final NodeList nodeList = mock(NodeList.class);
    doReturn(descriptionNlCount).when(nodeList).getLength();
    doReturn(descElementMock).when(nodeList).item(0);
    doReturn(nodeList).when(elementMock).getElementsByTagName(PmdCatalogRule.ATTR_DESCRIPTION);

    return elementMock;
  }
}

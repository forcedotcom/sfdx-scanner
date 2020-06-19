package sfdc.sfdx.scanner.pmd;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static sfdc.sfdx.scanner.TestConstants.*;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import sfdc.sfdx.scanner.messaging.EventKey;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogJson;

/**
 * Unit test for {@link PmdRuleCataloger}
 */
public class PmdRuleCatalogerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();;

    @SuppressWarnings("unchecked")
	@Test
	public void testAddJar() {
		Map<String, List<String>> rulePathEntries = new Hashtable<>();
		
		rulePathEntries.put(APEX, Collections.singletonList(JAR_FILE_CATEGORIES_AND_RULESETS.toAbsolutePath().toString()));
		PmdRuleCataloger pmdRuleCataloger = new PmdRuleCataloger(rulePathEntries);
		PmdRuleCataloger pmdRuleCatalogerSpy = Mockito.spy(pmdRuleCataloger);
		
		ArgumentCaptor<PmdCatalogJson> pmdCatalogJsonCaptor = ArgumentCaptor.forClass(PmdCatalogJson.class);
		Mockito.doNothing().when(pmdRuleCatalogerSpy).writeJsonToFile(pmdCatalogJsonCaptor.capture());
		pmdRuleCatalogerSpy.catalogRules();
		
		PmdCatalogJson pmdCatalogJson = pmdCatalogJsonCaptor.getValue();
		assertNotNull(pmdCatalogJson);
		
		JSONObject json = pmdCatalogJson.constructJson();
		assertNotNull(json);
		
		List<Object> rulesets = (List<Object>)json.get(PmdCatalogJson.JSON_RULESETS);
		assertNotNull(rulesets);
		assertThat(rulesets, hasSize(equalTo(1)));
		Map<String, Object> ruleset = (Map<String, Object>)rulesets.get(0);
		assertNotNull(ruleset);
		assertThat((List<String>)ruleset.get(PmdCatalogJson.JSON_PATHS), contains("rulesets/apex/rules1.xml"));

		List<Object> categories = (List<Object>)json.get(PmdCatalogJson.JSON_CATEGORIES);
		assertNotNull(categories);
		assertThat(categories, hasSize(equalTo(1)));
		Map<String, Object> category = (Map<String, Object>)categories.get(0);
		assertNotNull(category);
		assertThat((List<String>)category.get(PmdCatalogJson.JSON_PATHS), contains("category/apex/cat1.xml"));
    }
    
    @SuppressWarnings("unchecked")
	@Test
	public void testAddXml() {
    	String path = XML_FILE.toAbsolutePath().toString();
		Map<String, List<String>> rulePathEntries = new Hashtable<>();
		
		rulePathEntries.put(APEX, Collections.singletonList(path));
		PmdRuleCataloger pmdRuleCataloger = new PmdRuleCataloger(rulePathEntries);
		PmdRuleCataloger pmdRuleCatalogerSpy = Mockito.spy(pmdRuleCataloger);
		
		ArgumentCaptor<PmdCatalogJson> pmdCatalogJsonCaptor = ArgumentCaptor.forClass(PmdCatalogJson.class);
		Mockito.doNothing().when(pmdRuleCatalogerSpy).writeJsonToFile(pmdCatalogJsonCaptor.capture());
		pmdRuleCatalogerSpy.catalogRules();
		
		PmdCatalogJson pmdCatalogJson = pmdCatalogJsonCaptor.getValue();
		assertNotNull(pmdCatalogJson);

		JSONObject json = pmdCatalogJson.constructJson();
		assertNotNull(json);

		List<Object> categories = (List<Object>)json.get(PmdCatalogJson.JSON_CATEGORIES);
		assertNotNull(categories);
		assertThat(categories, hasSize(equalTo(1)));
		Map<String, Object> category = (Map<String, Object>)categories.get(0);
		assertNotNull(category);
		assertThat((List<String>)category.get(PmdCatalogJson.JSON_PATHS), contains(path));
    }
    
    @Test
	public void testExceptionIsThrownWhenCollisionOccurs() {
		Map<String, List<String>> rulePathEntries = new Hashtable<>();
		
		rulePathEntries.put(APEX, Collections.singletonList(COLLISION_DIR.toAbsolutePath().toString()));
		PmdRuleCataloger pmdRuleCataloger = new PmdRuleCataloger(rulePathEntries);
		
		thrown.expect(new SfdxScannerExceptionMatcher(EventKey.ERROR_EXTERNAL_DUPLICATE_XML_PATH,
				new String[] { "category/joshapex/somecat.xml", COLLISION_JAR_2.toAbsolutePath().toString(),
						COLLISION_JAR_1.toAbsolutePath().toString() }));

		pmdRuleCataloger.catalogRules();
	}
}

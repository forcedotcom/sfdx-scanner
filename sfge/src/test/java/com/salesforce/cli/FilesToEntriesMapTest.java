package com.salesforce.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.HashSet;
import org.hamcrest.Matchers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

public class FilesToEntriesMapTest {

    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";
    private static final String FILE_3 = "file3";
    private static final String ENTRY_FILE_1 = "entryFile1";
    private static final String ENTRY_FILE_2 = "entryFile2";
    private static final String ENTRY_METHOD_1 = "entryMethod1";
    private static final String ENTRY_METHOD_2 = "entryMethod2";
    private static final String ENTRY_METHOD_3 = "entryMethod3";

    @Test
    public void testEntryCreation() {
        FilesToEntriesMap filesToEntriesMap = new FilesToEntriesMap();
        filesToEntriesMap.put(FILE_1, ENTRY_FILE_1, ENTRY_METHOD_1);
        filesToEntriesMap.put(FILE_2, ENTRY_FILE_1, ENTRY_METHOD_2);
        filesToEntriesMap.put(FILE_1, ENTRY_FILE_2, ENTRY_METHOD_3);

        HashMap<String, HashSet<FilesToEntriesMap.Entry>> actualMap = filesToEntriesMap.getMap();
        assertThat(actualMap.keySet(), containsInAnyOrder(FILE_1, FILE_2));

        HashSet<FilesToEntriesMap.Entry> entries1 = actualMap.get(FILE_1);
        assertThat(
                entries1,
                containsInAnyOrder(
                        new FilesToEntriesMap.Entry(ENTRY_FILE_1, ENTRY_METHOD_1),
                        new FilesToEntriesMap.Entry(ENTRY_FILE_2, ENTRY_METHOD_3)));

        HashSet<FilesToEntriesMap.Entry> entries2 = actualMap.get(FILE_2);
        assertThat(entries2, contains(new FilesToEntriesMap.Entry(ENTRY_FILE_1, ENTRY_METHOD_2)));
    }

    @Test
    public void testMerge_appendingToExistingFilename() {
        FilesToEntriesMap filesToEntriesMap1 = new FilesToEntriesMap();
        filesToEntriesMap1.put(FILE_1, ENTRY_FILE_1, ENTRY_METHOD_1);
        filesToEntriesMap1.put(FILE_2, ENTRY_FILE_1, ENTRY_METHOD_2);
        filesToEntriesMap1.put(FILE_1, ENTRY_FILE_2, ENTRY_METHOD_3);

        FilesToEntriesMap filesToEntriesMap2 = new FilesToEntriesMap();
        filesToEntriesMap2.put(FILE_2, ENTRY_FILE_2, ENTRY_METHOD_3);

        filesToEntriesMap1.merge(filesToEntriesMap2);

        HashMap<String, HashSet<FilesToEntriesMap.Entry>> actualMap = filesToEntriesMap1.getMap();

        HashSet<FilesToEntriesMap.Entry> entries2 = actualMap.get(FILE_2);
        assertThat(
                entries2,
                contains(
                        new FilesToEntriesMap.Entry(ENTRY_FILE_1, ENTRY_METHOD_2),
                        new FilesToEntriesMap.Entry(ENTRY_FILE_2, ENTRY_METHOD_3)));
    }

    @Test
    public void testMerge_addingNewFilename() {
        FilesToEntriesMap filesToEntriesMap1 = new FilesToEntriesMap();
        filesToEntriesMap1.put(FILE_1, ENTRY_FILE_1, ENTRY_METHOD_1);
        filesToEntriesMap1.put(FILE_2, ENTRY_FILE_1, ENTRY_METHOD_2);
        filesToEntriesMap1.put(FILE_1, ENTRY_FILE_2, ENTRY_METHOD_3);

        FilesToEntriesMap filesToEntriesMap2 = new FilesToEntriesMap();
        filesToEntriesMap2.put(FILE_3, ENTRY_FILE_2, ENTRY_METHOD_3);

        filesToEntriesMap1.merge(filesToEntriesMap2);

        HashMap<String, HashSet<FilesToEntriesMap.Entry>> actualMap = filesToEntriesMap1.getMap();

        HashSet<FilesToEntriesMap.Entry> entries3 = actualMap.get(FILE_3);
        assertThat(entries3, contains(new FilesToEntriesMap.Entry(ENTRY_FILE_2, ENTRY_METHOD_3)));
    }

    @Test
    public void testJsonString() throws ParseException {
        FilesToEntriesMap filesToEntriesMap = new FilesToEntriesMap();
        filesToEntriesMap.put(FILE_1, ENTRY_FILE_1, ENTRY_METHOD_1);
        filesToEntriesMap.put(FILE_2, ENTRY_FILE_1, ENTRY_METHOD_2);
        filesToEntriesMap.put(FILE_1, ENTRY_FILE_2, ENTRY_METHOD_3);

        String jsonString = filesToEntriesMap.toJsonString();
        JSONParser parser = new JSONParser();
        JSONObject data = (JSONObject) parser.parse(jsonString);

        assertThat(data.containsKey("data"), Matchers.equalTo(true));

        JSONArray filesToEntriesMapData = (JSONArray) data.get(FilesToEntriesMap.FIELD_DATA);
        assertThat(filesToEntriesMapData.size(), equalTo(2));

        JSONObject file2Data = (JSONObject) filesToEntriesMapData.get(0);
        assertThat(file2Data.get(FilesToEntriesMap.FIELD_FILENAME), equalTo(FILE_2));
        JSONArray entries2 = (JSONArray) file2Data.get(FilesToEntriesMap.FIELD_ENTRIES);
        assertThat(entries2.size(), equalTo(1));
    }
}

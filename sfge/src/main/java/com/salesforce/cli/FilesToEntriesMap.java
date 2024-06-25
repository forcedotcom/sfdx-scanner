package com.salesforce.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Mapping of files that occur in an {@link com.salesforce.graph.ApexPath} and the entry method from
 * which the path originates.
 */
public class FilesToEntriesMap {
    @VisibleForTesting static final String FIELD_DATA = "data";
    @VisibleForTesting static final String FIELD_FILENAME = "filename";
    @VisibleForTesting static final String FIELD_ENTRIES = "entries";
    private final HashMap<String, HashSet<Entry>> map;

    public FilesToEntriesMap() {
        this.map = new HashMap<>();
    }

    /**
     * Creates a new mapping of the file name and the corresponding entry method it impacts
     *
     * @param filename that a path touches
     * @param entryFile of the entry method
     * @param entryMethod of the entry method
     */
    public void put(String filename, String entryFile, String entryMethod) {
        final Entry entry = new Entry(entryFile, entryMethod);
        final HashSet<Entry> value = map.computeIfAbsent(filename, key -> new HashSet<>());
        value.add(entry);
    }

    /**
     * Merges entries of another {@link FilesToEntriesMap} instance into the current instance.
     *
     * @param other {@link FilesToEntriesMap} instance to merge
     */
    public void merge(FilesToEntriesMap other) {
        for (Map.Entry<String, HashSet<Entry>> otherMapEntry : other.map.entrySet()) {
            map.merge(
                    otherMapEntry.getKey(),
                    otherMapEntry.getValue(),
                    (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
        }
    }

    /**
     * @return JSON data representing the state of this instance
     */
    public String toJsonString() {
        final JSONArray data = new JSONArray();
        for (String filename : map.keySet()) {
            data.add(getFileToEntriesJson(filename));
        }
        final JSONObject mapping = new JSONObject();
        mapping.put(FIELD_DATA, data);

        return mapping.toJSONString();
    }

    private JSONObject getFileToEntriesJson(String filename) {
        final HashSet<Entry> entries = map.get(filename);
        final JSONArray entriesJson = new JSONArray();
        for (Entry entry : entries) {
            entriesJson.add(entry.toString());
        }
        final JSONObject fileToEntriesJson = new JSONObject();
        fileToEntriesJson.put(FIELD_FILENAME, filename);
        fileToEntriesJson.put(FIELD_ENTRIES, entriesJson);

        return fileToEntriesJson;
    }

    @VisibleForTesting
    HashMap<String, HashSet<Entry>> getMap() {
        return map;
    }

    @VisibleForTesting
    static class Entry {
        private final String filename;
        private final String methodName;

        // Graph Engine CLI understands this format (filename#method)
        // and handles the target method as an entry point
        private static final String FORMAT = "%s#%s";

        Entry(String filename, String methodName) {
            this.filename = filename;
            this.methodName = methodName;
        }

        @Override
        public String toString() {
            return String.format(FORMAT, this.filename, this.methodName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(filename, entry.filename) && Objects.equals(methodName, entry.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, methodName);
        }
    }
}

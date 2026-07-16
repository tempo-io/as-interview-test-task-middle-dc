package io.tempo.assignment.export.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One flat output record: an ordered map of {@code fieldId -> value}.
 *
 * <p>The iteration order of {@link #getValues()} must match the order of the field ids
 * requested in the {@link ExportRequest}.
 */
public final class ExportRow {

    private final Map<String, Object> values;

    public ExportRow(Map<String, Object> values) {
        this.values = values == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** Ordered, unmodifiable view of the row's values keyed by field id. */
    public Map<String, Object> getValues() {
        return values;
    }

    public Object get(String fieldId) {
        return values.get(fieldId);
    }

    @Override
    public String toString() {
        return "ExportRow" + values;
    }
}

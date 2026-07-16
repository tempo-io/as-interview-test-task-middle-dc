package io.tempo.assignment.export.model;

import java.util.Collections;
import java.util.List;

/**
 * Describes a single export: which fields to project (ordered) and the page size to use
 * when reading from the page source.
 */
public final class ExportRequest {

    private final List<String> fields;
    private final int maxPerPage;

    public ExportRequest(List<String> fields, int maxPerPage) {
        this.fields = fields;
        this.maxPerPage = maxPerPage;
    }

    /** Ordered list of field ids to export. May be {@code null} — decide how to handle it. */
    public List<String> getFields() {
        return fields == null ? null : Collections.unmodifiableList(fields);
    }

    /** Page size used when reading a page from the source. */
    public int getMaxPerPage() {
        return maxPerPage;
    }
}

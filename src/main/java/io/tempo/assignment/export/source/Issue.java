package io.tempo.assignment.export.source;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a single Jira issue as read from a page source.
 *
 * <p>Besides the built-in properties, an issue carries an ordered map of custom-field
 * values keyed by field id (e.g. {@code "priority" -> "High"}).
 */
public final class Issue {

    private final long id;
    private final String key;
    private final String projectKey;
    private final String summary;
    private final String status;
    private final Instant updated;
    private final Map<String, Object> customFields;

    public Issue(long id,
                 String key,
                 String projectKey,
                 String summary,
                 String status,
                 Instant updated,
                 Map<String, Object> customFields) {
        this.id = id;
        this.key = key;
        this.projectKey = projectKey;
        this.summary = summary;
        this.status = status;
        this.updated = updated;
        this.customFields = customFields == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(customFields));
    }

    public long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getStatus() {
        return status;
    }

    public Instant getUpdated() {
        return updated;
    }

    /** Never {@code null}; unmodifiable. */
    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Issue issue = (Issue) o;
        return id == issue.id
                && Objects.equals(key, issue.key)
                && Objects.equals(projectKey, issue.projectKey)
                && Objects.equals(summary, issue.summary)
                && Objects.equals(status, issue.status)
                && Objects.equals(updated, issue.updated)
                && Objects.equals(customFields, issue.customFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, projectKey, summary, status, updated, customFields);
    }

    @Override
    public String toString() {
        return "Issue{id=" + id + ", key='" + key + '\'' + ", updated=" + updated + '}';
    }
}

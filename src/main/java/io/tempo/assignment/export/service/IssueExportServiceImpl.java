package io.tempo.assignment.export.service;

import io.tempo.assignment.export.model.ExportRequest;
import io.tempo.assignment.export.model.ExportRow;
import io.tempo.assignment.export.source.Issue;
import io.tempo.assignment.export.source.IssuePageSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads every issue from {@link #source}, de-duplicates by id (newest {@code updated} wins,
 * ties/nulls keep the first seen), and projects the survivors into {@link ExportRow}s in
 * first-seen order, per the fields listed on the {@link ExportRequest}.
 *
 * <p>Java 8 only. Do not change the constructor signature or the interface it implements.
 */
public class IssueExportServiceImpl implements IssueExportService {

    private final IssuePageSource source;

    public IssueExportServiceImpl(IssuePageSource source) {
        this.source = source;
    }

    @Override
    public List<ExportRow> export(ExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        List<String> fields = request.getFields();
        if (fields == null) {
            throw new IllegalArgumentException("request.fields must not be null");
        }
        if (request.getMaxPerPage() < 1) {
            throw new IllegalArgumentException(
                    "maxPerPage must be >= 1, got " + request.getMaxPerPage());
        }

        List<Issue> rawIssues = fetchAll(request.getMaxPerPage());
        Map<Long, Issue> byId = deduplicate(rawIssues);
        return project(byId.values(), fields);
    }

    /**
     * Reads every raw row from {@link #source}, in page order, duplicate ids included.
     *
     * <p>Starts at offset 0 and advances by the number of rows actually returned; stops the
     * first time a page comes back shorter than {@code maxPerPage} (including empty) and never
     * calls the source again after that. Assumes {@code maxPerPage >= 1}, guaranteed by request
     * validation before this is called.
     */
    List<Issue> fetchAll(int maxPerPage) {
        List<Issue> all = new ArrayList<>();
        int startAt = 0;
        while (true) {
            List<Issue> page = source.fetchPage(startAt, maxPerPage);
            all.addAll(page);
            if (page.size() < maxPerPage) {
                break;
            }
            startAt += page.size();
        }
        return all;
    }

    /**
     * Collapses raw, possibly-duplicated rows into one {@link Issue} per id: the occurrence
     * with the newest {@code updated} wins; a tie or a {@code null} on either side keeps
     * whichever occurrence was seen first.
     *
     * <p>Backed by a {@link LinkedHashMap} so insertion order (first-seen order) is preserved
     * for Task 3 — {@code put} on an existing key updates the value without moving the entry.
     */
    Map<Long, Issue> deduplicate(List<Issue> rawIssues) {
        Map<Long, Issue> byId = new LinkedHashMap<>();
        for (Issue issue : rawIssues) {
            Issue existing = byId.get(issue.getId());
            if (existing == null || isNewer(issue.getUpdated(), existing.getUpdated())) {
                byId.put(issue.getId(), issue);
            }
        }
        return byId;
    }

    private boolean isNewer(Instant candidate, Instant current) {
        return candidate != null && current != null && candidate.isAfter(current);
    }

    /** Projects each issue, in the given (first-seen) order, into an {@link ExportRow}. */
    List<ExportRow> project(Collection<Issue> issues, List<String> fields) {
        List<ExportRow> rows = new ArrayList<>();
        for (Issue issue : issues) {
            rows.add(projectOne(issue, fields));
        }
        return rows;
    }

    private ExportRow projectOne(Issue issue, List<String> fields) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String fieldId : fields) {
            values.put(fieldId, resolveField(issue, fieldId));
        }
        return new ExportRow(values);
    }

    private Object resolveField(Issue issue, String fieldId) {
        switch (fieldId) {
            case "id": return issue.getId();
            case "key": return issue.getKey();
            case "projectKey": return issue.getProjectKey();
            case "summary": return issue.getSummary();
            case "status": return issue.getStatus();
            case "updated": return issue.getUpdated();
            default: return issue.getCustomFields().get(fieldId);
        }
    }
}

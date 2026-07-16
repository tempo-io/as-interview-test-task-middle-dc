package io.tempo.assignment.export.source;

import java.util.ArrayList;
import java.util.List;

/**
 * A trivial in-memory {@link IssuePageSource} backed by a fixed list of issues.
 *
 * <p>It simply slices the backing list, so if the same issue id appears more than once in
 * the list it will be returned more than once (possibly on different pages) — which is
 * exactly how the production sources behave when their query joins one-to-many tables.
 */
public final class InMemoryIssuePageSource implements IssuePageSource {

    private final List<Issue> issues;

    public InMemoryIssuePageSource(List<Issue> issues) {
        this.issues = new ArrayList<>(issues);
    }

    @Override
    public List<Issue> fetchPage(int startAt, int maxPerPage) {
        if (startAt >= issues.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(startAt + maxPerPage, issues.size());
        return new ArrayList<>(issues.subList(startAt, end));
    }
}

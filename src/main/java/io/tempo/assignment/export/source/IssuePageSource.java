package io.tempo.assignment.export.source;

import java.util.List;

/**
 * Reads issues from an underlying store one page at a time.
 *
 * <p>Implementations back onto large tables (potentially millions of rows), so callers
 * must page through the data instead of loading it all at once.
 */
public interface IssuePageSource {

    /**
     * Returns at most {@code maxPerPage} issues starting at the zero-based offset
     * {@code startAt}.
     *
     * <p>Contract:
     * <ul>
     *   <li>Ordering is stable across calls for the same underlying data.</li>
     *   <li>A returned page with fewer than {@code maxPerPage} issues (including an empty
     *       list) indicates that there is no more data after it.</li>
     *   <li>Because the underlying query joins one-to-many tables, the <b>same issue id
     *       may appear on more than one page</b>.</li>
     * </ul>
     *
     * @param startAt    zero-based offset of the first issue to return
     * @param maxPerPage maximum number of issues to return (always {@code >= 1})
     * @return the page of issues; never {@code null}
     */
    List<Issue> fetchPage(int startAt, int maxPerPage);
}

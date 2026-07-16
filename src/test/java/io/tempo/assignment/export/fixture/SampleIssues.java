package io.tempo.assignment.export.fixture;

import io.tempo.assignment.export.source.InMemoryIssuePageSource;
import io.tempo.assignment.export.source.Issue;
import io.tempo.assignment.export.source.IssuePageSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shared, fixed data fixture for the assignment. See {@code FIXTURE.md} for the full
 * description and the expected export result.
 *
 * <p>{@link #rawRows()} is the list of rows exactly as the page source serves them,
 * <b>including deliberate duplicates</b> that model a one-to-many SQL join:
 *
 * <pre>
 *  #  id  key     project  status        updated       priority  storyPoints   note
 *  1   1  ABC-1   ABC      Done          2024-01-10    High      3
 *  2   2  ABC-2   ABC      In Progress   2024-01-11    Medium    -
 *  3   3  DEV-1   DEV      Done          2024-01-12    Low       5
 *  4   1  ABC-1   ABC      Done          2024-03-01    Critical  3             duplicate of #1, NEWER
 *  5   4  DEV-2   DEV      To Do         2024-01-13    -         -
 *  6   2  ABC-2   ABC      In Progress   (null)        Medium    -             duplicate of #2, null updated
 *  7   5  ABC-3   ABC      To Do         2024-02-01    High      -
 * </pre>
 *
 * Expected de-duplicated export (first-seen order, newest {@code updated} wins):
 * <pre>
 *  id=1  key=ABC-1  priority=Critical  updated=2024-03-01   (row #4 wins over #1)
 *  id=2  key=ABC-2  priority=Medium    updated=2024-01-11   (row #2 kept; #6 has null updated)
 *  id=3  key=DEV-1  priority=Low       updated=2024-01-12
 *  id=4  key=DEV-2  priority=null      updated=2024-01-13
 *  id=5  key=ABC-3  priority=High      updated=2024-02-01
 * </pre>
 */
public final class SampleIssues {

    private SampleIssues() {
    }

    // --- built-in field ids -----------------------------------------------------------
    public static final String F_ID = "id";
    public static final String F_KEY = "key";
    public static final String F_PROJECT_KEY = "projectKey";
    public static final String F_SUMMARY = "summary";
    public static final String F_STATUS = "status";
    public static final String F_UPDATED = "updated";

    // --- custom field ids -------------------------------------------------------------
    public static final String F_PRIORITY = "priority";
    public static final String F_STORY_POINTS = "storyPoints";

    // --- timestamps used by the fixture -----------------------------------------------
    public static final Instant JAN_10 = Instant.parse("2024-01-10T00:00:00Z");
    public static final Instant JAN_11 = Instant.parse("2024-01-11T00:00:00Z");
    public static final Instant JAN_12 = Instant.parse("2024-01-12T00:00:00Z");
    public static final Instant JAN_13 = Instant.parse("2024-01-13T00:00:00Z");
    public static final Instant FEB_01 = Instant.parse("2024-02-01T00:00:00Z");
    public static final Instant MAR_01 = Instant.parse("2024-03-01T00:00:00Z");

    /** Number of distinct issues once duplicates are collapsed. */
    public static final int DISTINCT_ISSUE_COUNT = 5;

    /** The raw rows as served by the source, in order, including duplicates. */
    public static List<Issue> rawRows() {
        List<Issue> rows = new ArrayList<>();
        rows.add(new Issue(1, "ABC-1", "ABC", "Login page", "Done", JAN_10,
                cf(F_PRIORITY, "High", F_STORY_POINTS, 3)));
        rows.add(new Issue(2, "ABC-2", "ABC", "Signup flow", "In Progress", JAN_11,
                cf(F_PRIORITY, "Medium")));
        rows.add(new Issue(3, "DEV-1", "DEV", "Build pipeline", "Done", JAN_12,
                cf(F_PRIORITY, "Low", F_STORY_POINTS, 5)));
        rows.add(new Issue(1, "ABC-1", "ABC", "Login page", "Done", MAR_01,
                cf(F_PRIORITY, "Critical", F_STORY_POINTS, 3)));
        rows.add(new Issue(4, "DEV-2", "DEV", "Nightly job", "To Do", JAN_13,
                cf()));
        rows.add(new Issue(2, "ABC-2", "ABC", "Signup flow", "In Progress", null,
                cf(F_PRIORITY, "Medium")));
        rows.add(new Issue(5, "ABC-3", "ABC", "Password reset", "To Do", FEB_01,
                cf(F_PRIORITY, "High")));
        return rows;
    }

    /** A page source over {@link #rawRows()}. */
    public static IssuePageSource newSource() {
        return new InMemoryIssuePageSource(rawRows());
    }

    /** Builds a custom-field map from alternating key/value pairs. */
    private static Map<String, Object> cf(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }
}

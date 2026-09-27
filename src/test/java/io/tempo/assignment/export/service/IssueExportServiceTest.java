package io.tempo.assignment.export.service;

import io.tempo.assignment.export.fixture.SampleIssues;
import io.tempo.assignment.export.model.ExportRequest;
import io.tempo.assignment.export.model.ExportRow;
import io.tempo.assignment.export.source.InMemoryIssuePageSource;
import io.tempo.assignment.export.source.Issue;
import io.tempo.assignment.export.source.IssuePageSource;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.tempo.assignment.export.fixture.SampleIssues.F_ID;
import static io.tempo.assignment.export.fixture.SampleIssues.F_KEY;
import static io.tempo.assignment.export.fixture.SampleIssues.F_PRIORITY;
import static io.tempo.assignment.export.fixture.SampleIssues.F_STORY_POINTS;
import static io.tempo.assignment.export.fixture.SampleIssues.F_UPDATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Acceptance tests for {@link IssueExportService}, run against the shared {@link SampleIssues}
 * fixture (see FIXTURE.md).
 *
 * <p>These are provided so you can verify your implementation immediately: they FAIL against
 * the skeleton and must all PASS once your implementation is correct. There is also a
 * {@code TODO} at the bottom asking you to add a couple of tests of your own — that part is
 * assessed too.
 */
public class IssueExportServiceTest {

    private IssueExportService serviceOverFixture() {
        return new IssueExportServiceImpl(SampleIssues.newSource());
    }

    private static ExportRequest request(int maxPerPage, String... fields) {
        return new ExportRequest(Arrays.asList(fields), maxPerPage);
    }

    private static List<Long> idsOf(List<ExportRow> rows) {
        List<Long> ids = new ArrayList<>();
        for (ExportRow row : rows) {
            ids.add((Long) row.get(F_ID));
        }
        return ids;
    }

    // --- pagination & de-duplication --------------------------------------------------

    @Test
    public void exportsEachDistinctIssueOnceInFirstSeenOrder() {
        List<ExportRow> rows = serviceOverFixture().export(request(2, F_ID));

        assertEquals(SampleIssues.DISTINCT_ISSUE_COUNT, rows.size());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), idsOf(rows));
    }

    @Test
    public void resultIsIndependentOfPageSize() {
        List<Long> expected = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        for (int pageSize : new int[]{1, 2, 3, 7, 100}) {
            List<ExportRow> rows = serviceOverFixture().export(request(pageSize, F_ID));
            assertEquals("page size " + pageSize, expected, idsOf(rows));
        }
    }

    @Test
    public void duplicateIssueKeepsMostRecentlyUpdatedValues() {
        List<ExportRow> rows = serviceOverFixture()
                .export(request(2, F_ID, F_PRIORITY, F_UPDATED));

        ExportRow issue1 = rows.get(0);
        assertEquals(1L, issue1.get(F_ID));
        assertEquals("Critical", issue1.get(F_PRIORITY));            // row #4 won
        assertEquals(SampleIssues.MAR_01, issue1.get(F_UPDATED));
    }

    @Test
    public void duplicateWithNullUpdatedDoesNotOverrideTheFirstSeenValue() {
        List<ExportRow> rows = serviceOverFixture().export(request(2, F_ID, F_UPDATED));

        ExportRow issue2 = rows.get(1);
        assertEquals(2L, issue2.get(F_ID));
        assertEquals(SampleIssues.JAN_11, issue2.get(F_UPDATED));    // #6 (null) did not win
    }

    // --- field projection -------------------------------------------------------------

    @Test
    public void projectsRequestedFieldsInOrderResolvingBuiltInCustomAndUnknown() {
        List<ExportRow> rows = serviceOverFixture()
                .export(request(10, F_KEY, F_PRIORITY, F_STORY_POINTS, "doesNotExist"));

        ExportRow first = rows.get(0); // issue id=1
        assertEquals(Arrays.asList(F_KEY, F_PRIORITY, F_STORY_POINTS, "doesNotExist"),
                new ArrayList<>(first.getValues().keySet())); // order preserved
        assertEquals("ABC-1", first.get(F_KEY));
        assertEquals("Critical", first.get(F_PRIORITY));
        assertEquals(3, first.get(F_STORY_POINTS));
        assertTrue(first.getValues().containsKey("doesNotExist")); // unknown key present...
        assertNull(first.get("doesNotExist"));                      // ...with a null value
    }

    @Test
    public void missingCustomFieldOnAnIssueIsNull() {
        List<ExportRow> rows = serviceOverFixture().export(request(10, F_ID, F_PRIORITY));

        ExportRow issue4 = rows.get(3); // id=4 has no custom fields
        assertEquals(4L, issue4.get(F_ID));
        assertNull(issue4.get(F_PRIORITY));
    }

    // --- edge cases & validation ------------------------------------------------------

    @Test
    public void emptySourceProducesNoRows() {
        IssueExportService service = new IssueExportServiceImpl(
                new InMemoryIssuePageSource(Collections.<Issue>emptyList()));

        assertTrue(service.export(request(10, F_ID)).isEmpty());
    }

    @Test
    public void rejectsNonPositivePageSize() {
        try {
            serviceOverFixture().export(request(0, F_ID));
            fail("expected IllegalArgumentException for maxPerPage < 1");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    // --- pagination (candidate-written) ------------------------------------------------

    /** A test double that returns fixed-size pages and counts how often it was called. */
    private static final class CountingFixedSizePageSource implements IssuePageSource {
        private final int[] pageSizes;
        private int calls = 0;

        CountingFixedSizePageSource(int... pageSizes) {
            this.pageSizes = pageSizes;
        }

        @Override
        public List<Issue> fetchPage(int startAt, int maxPerPage) {
            List<Issue> page = new ArrayList<>();
            if (calls < pageSizes.length) {
                for (int i = 0; i < pageSizes[calls]; i++) {
                    page.add(new Issue(startAt + i, "K-" + (startAt + i), "P", "s", "Open",
                            null, Collections.emptyMap()));
                }
            }
            calls++;
            return page;
        }

        int callCount() {
            return calls;
        }
    }

    @Test
    public void fetchAllStopsAfterFirstShortPageAndDoesNotCallSourceAgain() {
        CountingFixedSizePageSource source = new CountingFixedSizePageSource(2, 2, 2, 1);
        IssueExportServiceImpl service = new IssueExportServiceImpl(source);

        List<Issue> all = service.fetchAll(2);

        assertEquals(7, all.size());
        assertEquals(4, source.callCount());
    }

    @Test
    public void fetchAllReturnsAllRawRowsInOrderRegardlessOfPageSize() {
        IssueExportServiceImpl service = new IssueExportServiceImpl(SampleIssues.newSource());

        List<Issue> all = service.fetchAll(3);

        assertEquals(SampleIssues.rawRows(), all);
    }

    @Test
    public void fetchAllOnEmptySourceReturnsEmptyAfterOneCall() {
        CountingFixedSizePageSource source = new CountingFixedSizePageSource();
        IssueExportServiceImpl service = new IssueExportServiceImpl(source);

        List<Issue> all = service.fetchAll(10);

        assertTrue(all.isEmpty());
        assertEquals(1, source.callCount());
    }

    // --- field projection (candidate-written) ------------------------------------------

    @Test
    public void projectsFieldsInRequestedOrderEvenWhenNotDefaultOrder() {
        List<ExportRow> rows = serviceOverFixture()
                .export(request(10, F_PRIORITY, F_ID, F_KEY));

        ExportRow first = rows.get(0); // issue id=1
        assertEquals(Arrays.asList(F_PRIORITY, F_ID, F_KEY),
                new ArrayList<>(first.getValues().keySet()));
    }

    @Test
    public void emptyFieldListProducesOneEmptyRowPerDistinctIssue() {
        List<ExportRow> rows = serviceOverFixture().export(request(10));

        assertEquals(SampleIssues.DISTINCT_ISSUE_COUNT, rows.size());
        for (ExportRow row : rows) {
            assertTrue(row.getValues().isEmpty());
        }
    }

    // --- validation (candidate-written) -------------------------------------------------

    @Test
    public void rejectsNegativePageSize() {
        try {
            serviceOverFixture().export(request(-1, F_ID));
            fail("expected IllegalArgumentException for maxPerPage < 1");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void rejectsNullRequest() {
        try {
            serviceOverFixture().export(null);
            fail("expected IllegalArgumentException for a null request");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void rejectsRequestWithNullFields() {
        try {
            serviceOverFixture().export(new ExportRequest(null, 10));
            fail("expected IllegalArgumentException for null fields");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void emptyFieldsListIsNotTreatedAsNullFields() {
        // Negative control: an empty field list is Task 4's valid "no columns" case, distinct
        // from the null-fields rejection above — must not throw.
        List<ExportRow> rows = serviceOverFixture()
                .export(new ExportRequest(Collections.emptyList(), 10));

        assertEquals(SampleIssues.DISTINCT_ISSUE_COUNT, rows.size());
    }

    // --- additional de-duplication & pagination edge cases (candidate-written) ---------

    private static Issue issue(long id, String key, Instant updated, String priority) {
        Map<String, Object> customFields = new LinkedHashMap<>();
        customFields.put(F_PRIORITY, priority);
        return new Issue(id, key, "P", "s", "Open", updated, customFields);
    }

    @Test
    public void duplicateWithEqualNonNullUpdatedKeepsTheFirstSeenValue() {
        // Spec 3.2: "equal (or null)" both fall back to first-seen. The shared fixture only
        // exercises the null half (issue #2/#6); this covers the equal-non-null half.
        Instant sameInstant = SampleIssues.JAN_11;
        IssuePageSource source = new InMemoryIssuePageSource(Arrays.asList(
                issue(1, "K-1", sameInstant, "First"),
                issue(1, "K-1", sameInstant, "Second")));
        IssueExportService service = new IssueExportServiceImpl(source);

        List<ExportRow> rows = service.export(request(10, F_ID, F_PRIORITY));

        assertEquals(1, rows.size());
        assertEquals("First", rows.get(0).get(F_PRIORITY));
    }

    @Test
    public void tripleDuplicateKeepsTheOverallNewestNotJustTheLastComparison() {
        // Three occurrences of the same id, with the newest one in the middle of the run —
        // catches an implementation that folds pairwise against the previous raw row instead
        // of against the current best-seen winner.
        IssuePageSource source = new InMemoryIssuePageSource(Arrays.asList(
                issue(1, "K-1", SampleIssues.JAN_10, "Low"),
                issue(1, "K-1", SampleIssues.MAR_01, "Critical"),   // overall newest
                issue(1, "K-1", SampleIssues.JAN_12, "Medium")));   // newer than #1, older than #2
        IssueExportService service = new IssueExportServiceImpl(source);

        List<ExportRow> rows = service.export(request(10, F_ID, F_PRIORITY, F_UPDATED));

        assertEquals(1, rows.size());
        assertEquals("Critical", rows.get(0).get(F_PRIORITY));
        assertEquals(SampleIssues.MAR_01, rows.get(0).get(F_UPDATED));
    }

    @Test
    public void fetchAllMakesOneExtraEmptyCallWhenLastRealPageIsExactlyFull() {
        // When the last real page happens to be exactly maxPerPage in size, the "short page
        // means stop" rule can't apply to it — the implementation must page once more and see
        // an empty page before it stops.
        CountingFixedSizePageSource source = new CountingFixedSizePageSource(2, 2, 2, 2);
        IssueExportServiceImpl service = new IssueExportServiceImpl(source);

        List<Issue> all = service.fetchAll(2);

        assertEquals(8, all.size());
        assertEquals(5, source.callCount());
    }

    @Test
    public void repeatedFieldIdInRequestCollapsesToASingleRowEntry() {
        // Not forbidden by the spec, but surprising: ExportRow is backed by a LinkedHashMap
        // keyed by field id, so requesting the same id twice yields one entry, not two. This
        // pins down that documented behavior (see NOTES.md).
        List<ExportRow> rows = serviceOverFixture().export(request(10, F_ID, F_ID));

        ExportRow first = rows.get(0);
        assertEquals(1, first.getValues().size());
        assertEquals(Collections.singletonList(F_ID), new ArrayList<>(first.getValues().keySet()));
    }

    @Test
    public void explicitNullCustomFieldValueIsIndistinguishableFromAMissingOne() {
        Map<String, Object> withExplicitNull = new LinkedHashMap<>();
        withExplicitNull.put(F_PRIORITY, null);
        IssuePageSource source = new InMemoryIssuePageSource(Arrays.asList(
                new Issue(1, "K-1", "P", "s", "Open", null, withExplicitNull),
                new Issue(2, "K-2", "P", "s", "Open", null, Collections.emptyMap())));
        IssueExportService service = new IssueExportServiceImpl(source);

        List<ExportRow> rows = service.export(request(10, F_ID, F_PRIORITY));

        assertNull(rows.get(0).get(F_PRIORITY)); // explicit null value
        assertNull(rows.get(1).get(F_PRIORITY)); // key entirely absent
        assertTrue(rows.get(0).getValues().containsKey(F_PRIORITY));
        assertTrue(rows.get(1).getValues().containsKey(F_PRIORITY));
    }
}

package io.tempo.assignment.export.service;

import io.tempo.assignment.export.fixture.SampleIssues;
import io.tempo.assignment.export.model.ExportRequest;
import io.tempo.assignment.export.model.ExportRow;
import io.tempo.assignment.export.source.InMemoryIssuePageSource;
import io.tempo.assignment.export.source.Issue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    // --- TODO (candidate) -------------------------------------------------------------
    // Add at least two tests of your own that pin down behaviour the tests above do not
    // cover. Ideas: an empty field list; a source that is queried the right number of
    // times (use a counting test double); a null request. Choose what you think matters.
}

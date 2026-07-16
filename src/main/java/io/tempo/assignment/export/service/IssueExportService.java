package io.tempo.assignment.export.service;

import io.tempo.assignment.export.model.ExportRequest;
import io.tempo.assignment.export.model.ExportRow;

import java.util.List;

/**
 * Pulls all issues from an issue page source, de-duplicates them, and projects each
 * surviving issue into an {@link ExportRow} according to the {@link ExportRequest}.
 */
public interface IssueExportService {

    /**
     * Executes the export and returns the projected rows.
     *
     * @param request the export request (fields to project + page size)
     * @return the export rows, in deterministic order (see the assignment spec)
     */
    List<ExportRow> export(ExportRequest request);
}

package io.tempo.assignment.export.service;

import io.tempo.assignment.export.model.ExportRequest;
import io.tempo.assignment.export.model.ExportRow;
import io.tempo.assignment.export.source.IssuePageSource;

import java.util.List;

/**
 * TODO (candidate): implement this class.
 *
 * <p>See ASSIGNMENT.md, section 3, for the full contract:
 * <ul>
 *   <li>page through {@link #source} until a short/empty page is returned;</li>
 *   <li>de-duplicate by issue id, keeping the most recently updated occurrence;</li>
 *   <li>preserve first-seen order;</li>
 *   <li>project each issue into an {@link ExportRow} using the requested fields;</li>
 *   <li>validate the request.</li>
 * </ul>
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
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

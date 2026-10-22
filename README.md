# Home Assignment — Issue Export Paginator & Aggregator

**Role:** Middle Java Developer — DC BI Connectors
**Expected effort:** 2–3 hours
**Language / stack:** Java 8, JUnit (Maven). No database, no Jira SDK, no network — everything runs in memory.

---

## 1. Background

Our product exports Jira data (issues, worklogs, budgets, custom fields …) into external
BI tools such as Power BI, Tableau, BigQuery and SQL databases.

Every data source in our system follows the same shape:

1. A **page source** reads rows from the underlying store **one page at a time** (the store
   can hold millions of rows, so we never load everything at once).
2. An **export service** pulls all the pages, cleans the data up, and projects each record
   into a flat **export row** — an ordered set of `fieldId → value` pairs that the BI tool
   consumes as table columns.

You are given the data model and the interfaces. **Your job is to implement the export
service and to test it.** The task is deliberately small in surface area but has several
realistic edge cases that we care about in production.

---

## 2. What you get (starter project)

A ready-to-build Maven project under `starter/`. Import it into your IDE and run
`mvn test`. It is organised into small packages:

```
src/main/java/io/tempo/assignment/export/
├── model/     ExportRequest, ExportRow                 (the export request & output row)
├── source/    Issue, IssuePageSource (interface),      (issues and where they come from)
│              InMemoryIssuePageSource (a simple impl)
└── service/   IssueExportService (interface),
               IssueExportServiceImpl  ← implement this

src/test/java/io/tempo/assignment/export/
├── fixture/   SampleIssues            (the shared test data — see FIXTURE.md)
└── service/   IssueExportServiceTest  (real tests — see below)
```

Key files:

| File | What it is |
|------|------------|
| `service/IssueExportServiceImpl.java` | **Skeleton — this is what you implement.** |
| `service/IssueExportService.java`, `source/IssuePageSource.java` | Interfaces — **do not change their signatures**; we run your solution against them. |
| `model/*` | The `ExportRequest` and `ExportRow` data types. |
| `source/Issue.java`, `source/InMemoryIssuePageSource.java` | The `Issue` type and a simple in-memory source, used by the fixture. |
| `fixture/SampleIssues.java` + `FIXTURE.md` | A **prepared, documented dataset** the tests run against. |
| `service/IssueExportServiceTest.java` | **Real acceptance tests** — they fail against the skeleton and must pass once your code is correct. Includes a `TODO` asking you to add a couple of your own. |

You may add classes and packages, but keep the existing interface signatures intact.

---

## 3. The contract you must implement

```java
public interface IssueExportService {
    List<ExportRow> export(ExportRequest request);
}
```

`IssueExportServiceImpl` receives an `IssuePageSource` via its constructor.

### 3.1 Pagination

Read from the source page by page using its zero-based offset:

```java
List<Issue> fetchPage(int startAt, int maxPerPage);
```

- Start at `startAt = 0` and advance by the size of the page you received.
- The source returns **at most** `maxPerPage` issues.
- A page **shorter than `maxPerPage`** (including an empty page) means there is no more
  data — stop. **Do not call the source again after the last page.**
- `maxPerPage` comes from the request.

### 3.2 Deduplication

The underlying query joins one-to-many tables, so **the same issue id may appear on more
than one page**. The export must contain **each issue id at most once**.

When the same id appears multiple times:

- Keep the occurrence with the **most recent `updated`** timestamp.
- If two occurrences have equal (or `null`) timestamps, keep the **first one seen**.

### 3.3 Output order

The returned list must be **deterministic**: surviving issues appear in the order in which
their id was **first encountered** while paging. (Choosing a newer duplicate must not move
a row to the end.)

### 3.4 Field projection

`ExportRequest.getFields()` is an **ordered** list of field ids to export. For each surviving
issue, build an `ExportRow` whose values contain **exactly** those field ids, **in the same
order**. Resolve each field id as follows:

| Field id | Value |
|----------|-------|
| `id`, `key`, `projectKey`, `summary`, `status`, `updated` | the matching built-in property |
| any custom field id present in `Issue.getCustomFields()` | its value from that map |
| anything else (unknown field) | `null` — but the key must still be present in the row |

### 3.5 Validation

- `maxPerPage < 1` → throw `IllegalArgumentException`.
- Decide sensible behaviour for `null` request / `null` fields and be ready to justify it.

---

## 4. What we expect you to deliver

1. A working `IssueExportServiceImpl` satisfying section 3 — concretely, **all provided tests
   in `IssueExportServiceTest` must pass** (`mvn test` is green). They currently fail against
   the skeleton; that is expected until you implement the service.
2. **Your own additional tests.** The provided suite covers the main behaviour; the `TODO` at
   the bottom of `IssueExportServiceTest` asks you to add at least two more tests for cases
   you think matter (e.g. call-count on the source, empty field list, null request). This
   part is assessed — we want to see how you think about testing, not just that the given
   tests pass. Reuse the `SampleIssues` fixture or add to it.
3. Clean, readable, **Java 8-compatible** code (see constraint below).
4. A short note (a few lines in the PR description or a `NOTES.md`) covering:
   - any assumptions you made,
   - what you would add/change with more time.

### Java 8 constraint (important)

This mirrors our real codebase. Your code **must compile and run on Java 8**. In particular,
do **not** use: `List.of / Set.of / Map.of`, `String.isBlank`, `Optional.or / ifPresentOrElse`,
`var`, or any API added in Java 9+. Use the classic Java 8 equivalents.

---

## 5. Stretch goals (optional — only if you have time)

These are **not required**. We are more interested in a clean, correct core than a rushed
attempt at everything. Pick at most one and mention it in your notes:

- **S1 — Streaming instead of buffering.** `export` returns a `List`, so everything lives in
  memory. Sketch (or implement) an API that streams rows to a consumer so a 5-million-issue
  export does not run out of heap.
- **S2 — Keyset pagination.** Offset pagination (`startAt`) degrades badly on large tables.
  Describe how you would switch to keyset (a.k.a. seek) pagination on the issue id, and what
  the source interface would look like.

---

## 6. How to submit

- Send a zip of the project (must build with `mvn test`).
- Please keep the git history readable if you use a branch.

Good luck — and if any requirement is ambiguous, make a reasonable decision and write it
down in your notes. Being explicit about assumptions is part of what we assess.

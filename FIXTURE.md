# Test data fixture

The assignment ships with a **fixed, shared data fixture** so that the provided tests can
assert concrete, known results. You do **not** need to invent your own data to check
correctness — use this fixture (and add to it if you want extra cases).

- **Code:** `src/test/java/io/tempo/assignment/export/fixture/SampleIssues.java`
- `SampleIssues.rawRows()` — the rows exactly as the page source serves them (with duplicates).
- `SampleIssues.newSource()` — a page source over those rows.
- Field-id and timestamp constants (`F_ID`, `F_PRIORITY`, `MAR_01`, …) live on the same class.

## Raw rows (as served by the source, in order)

The rows contain **deliberate duplicates**, exactly as a one-to-many SQL join would produce
in production.

| #  | id | key   | project | status      | updated    | priority | storyPoints | note |
|----|----|-------|---------|-------------|------------|----------|-------------|------|
| 1  | 1  | ABC-1 | ABC     | Done        | 2024-01-10 | High     | 3           | |
| 2  | 2  | ABC-2 | ABC     | In Progress | 2024-01-11 | Medium   | –           | |
| 3  | 3  | DEV-1 | DEV     | Done        | 2024-01-12 | Low      | 5           | |
| 4  | 1  | ABC-1 | ABC     | Done        | 2024-03-01 | Critical | 3           | duplicate of #1, **newer** |
| 5  | 4  | DEV-2 | DEV     | To Do       | 2024-01-13 | –        | –           | no custom fields |
| 6  | 2  | ABC-2 | ABC     | In Progress | *(null)*   | Medium   | –           | duplicate of #2, **null updated** |
| 7  | 5  | ABC-3 | ABC     | To Do       | 2024-02-01 | High     | –           | |

7 raw rows → **5 distinct issues**.

## Expected de-duplicated export

De-duplicate by `id`, keep the occurrence with the newest `updated`, preserve **first-seen**
order:

| id | key   | priority | updated    | why |
|----|-------|----------|------------|-----|
| 1  | ABC-1 | Critical | 2024-03-01 | row #4 wins over #1 (newer `updated`) |
| 2  | ABC-2 | Medium   | 2024-01-11 | row #2 kept; #6 has `null` updated so it does not win |
| 3  | DEV-1 | Low      | 2024-01-12 | |
| 4  | DEV-2 | *(null)* | 2024-01-13 | `priority` not present on this issue → `null` |
| 5  | ABC-3 | High     | 2024-02-01 | |

This single fixture exercises every rule in the spec: multi-page paging, de-duplication,
newest-wins, null-timestamp handling, custom fields, a missing custom field, and an unknown
field id. The provided tests in `IssueExportServiceTest` assert exactly these results.

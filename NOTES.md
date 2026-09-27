# Notes

## Assumptions

- **`null` request, or a request with `null` fields, throws `IllegalArgumentException`.**
  This export path feeds scheduled/automated BI jobs (README §1) — a misconfigured request
  should fail loudly at call time, not silently produce an empty export that looks like "zero
  issues today" downstream. An **empty** `fields` list is a different, valid case: it means
  "export zero columns," and produces one empty `ExportRow` per surviving issue rather than
  throwing.
- **`maxPerPage < 1` throws `IllegalArgumentException`** before the source is ever called, per
  the spec.
- De-duplication ties (equal `updated`, or `null` on either side) keep the **first-seen**
  occurrence, per spec — implemented via `LinkedHashMap#put`, which updates an existing key's
  value without moving its position, so first-seen order falls out of the map for free rather
  than needing a separate ordering step.
- Field resolution: the six built-in ids (`id`, `key`, `projectKey`, `summary`, `status`,
  `updated`) are read from the matching `Issue` getter; any other id is looked up in
  `Issue.getCustomFields()`. A field id present in neither still appears in the row with value
  `null` (the key is always inserted, never skipped).

## Additional tests (beyond the required two) and why

The provided suite plus the first round of candidate tests already covered call-count pagination,
empty field lists, field reordering, and null/negative validation. A second pass turned up five
more cases worth pinning down, all added to `IssueExportServiceTest`:

- **`duplicateWithEqualNonNullUpdatedKeepsTheFirstSeenValue`** — spec 3.2's tie-break rule is
  "equal **or** null" `updated` values keep first-seen. `SampleIssues` only exercises the
  *null* half (issue #2 vs #6); nothing had two non-null, identical timestamps. Added directly
  rather than extending the shared fixture, since it only needs two rows built inline.
- **`tripleDuplicateKeepsTheOverallNewestNotJustTheLastComparison`** — every fixture id has at
  most 2 raw occurrences, so de-dup was only ever exercised pairwise. A 3-occurrence run with the
  newest row in the *middle* catches an implementation that folds each new row against the
  previous raw row instead of against the current best-seen winner (a plausible off-by-one for a
  hand-rolled loop, even though the reference `LinkedHashMap`-based implementation here happens to
  get it right either way).
- **`fetchAllMakesOneExtraEmptyCallWhenLastRealPageIsExactlyFull`** — `resultIsIndependentOfPageSize`
  already checks *correctness* for a page size that swallows all rows in one page, but nothing
  asserted *call count* for the case where the last real page is exactly `maxPerPage` long. That's
  the one case where "a page shorter than `maxPerPage` means stop" cannot fire on the last real
  page, so the implementation must issue one more call and see an empty page before stopping —
  easy to get wrong by stopping one call early.
- **`repeatedFieldIdInRequestCollapsesToASingleRowEntry`** — not forbidden by the spec, but
  surprising: `ExportRow` is backed by a `LinkedHashMap` keyed by field id, so requesting the same
  field id twice (e.g. `["id", "id"]`) silently collapses to one entry instead of two. Rather than
  changing behavior (a `List<Map.Entry<>>` would be a bigger, unrequested change to `ExportRow`),
  this test documents and locks in the current, reasonable behavior.
- **`explicitNullCustomFieldValueIsIndistinguishableFromAMissingOne`** — confirms an explicit
  `null` value in `Issue.getCustomFields()` and a wholly absent key both resolve to `null` in the
  row (both go through `Map.get`), so a future refactor to `containsKey`/`getOrDefault` semantics
  doesn't quietly change behavior for real (as opposed to missing) null-valued custom fields.

## What I'd add with more time

- Property-based/randomized tests for de-duplication (random shuffles of duplicate ids across
  page boundaries) to complement the fixed `SampleIssues` cases.
- A benchmark/stress test with a large synthetic source to sanity-check memory behavior, since
  `export()` currently buffers all pages and the full de-duplicated map in memory — fine for
  this assignment's in-memory scope, but worth flagging for a real, millions-of-rows source.

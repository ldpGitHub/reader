# Reading V5 Integration Design

Date: 2026-05-23
Last updated: 2026-05-24

## Core Decision

V5 is now a reading-page integrity pass, not an open-book gate.

The reader opens from a stable anchor catalog as soon as one readable anchor can
be resolved. It does not wait for two trusted sources before showing the table
of contents. Current chapter loading keeps priority. V5 runs in the background
as soon as reading has a trusted persisted tier/source evidence; background tier
fill continues without blocking V5. The output is written as chapter marks
instead of cropping the catalog.

Production flow:

```text
open reader
  -> resolve one anchor catalog
  -> show catalog immediately
  -> load current chapter and bounded prefetch
  -> restore/fill the dedicated tier from the source waterfall
  -> run or restore V5 for the primary trusted source as soon as one trusted source exists
  -> optionally probe a second source only when text is clearly dissimilar
  -> apply NORMAL / WRONG / NON_STORY / BAD_EXTRACTION / INCONCLUSIVE marks
```

Search and detail keep their existing behavior. They may fill in-memory tier
evidence, but they do not trigger V5.

## 2026-05-24 Implementation Status

The production reading integration has been implemented and pushed on
`codex/algorithmtest` as commit `3ade618 Implement V5 reading integration`.

Important status boundaries:

- Reading owns V5. Search and detail continue to use the existing source-engine
  paths and must not emit V5 epoch events.
- Catalog opening is anchor-first. The reader no longer waits for two trusted
  sources before showing the table of contents.
- V5 final marks are persisted and restored for unchanged catalog shape. V5
  intermediate analyzer artifacts are still not persisted.
- Quality/coherence failures are diagnostics and source-score evidence for
  current-chapter reading. They no longer hard-block displayable cleaned text.
- Hard content failures remain: missing detail, untrusted book detail, missing
  chapter, fingerprint rejection, null content, and empty cleaned content.

This last point is the current reading-content contract after the
`元始法则 / 第九百九十章 月白风清` device investigation. That chapter produced
low quality/coherence diagnostics, but it also produced non-empty cleaned body
text; the reader now renders it and records the quality problem instead of
staying in a terminal loading/error state.

## Implemented UX Contract

- Catalog display is anchor-first. A single anchor catalog is allowed when no
  trusted tier is ready yet.
- V5 never deletes rows from the stored catalog.
- Wrong/non-story/bad-extraction rows are visible by default.
- The catalog drawer has `显示错章`. Turning it off filters `WRONG`,
  `NON_STORY`, and `BAD_EXTRACTION` rows from the adapter view while preserving
  the current chapter row.
- Row rendering appends a small text marker:
  - `错` for `WRONG`
  - `抽` for `BAD_EXTRACTION`
  - `注` for `NON_STORY`
- `INCONCLUSIVE` is recorded but not shown as a strong warning.

## V5 State Mapping

| V5 mark | Meaning | UI behavior |
| --- | --- | --- |
| `NORMAL` | V5 judged the chapter usable for this book | normal row |
| `WRONG` | wrong-book story, polluted run, or polluted suffix | red `错` marker |
| `NON_STORY` | announcement, leave note, author note, postscript | muted `注` marker |
| `BAD_EXTRACTION` | shell, blank, preview-only, or broken extraction | red `抽` marker |
| `INCONCLUSIVE` | checked, but evidence is insufficient | normal row for now |

The mark is advisory for catalog UX and routing evidence. It is not a live text
rewriter.

## V5 Change Discipline

The migration target is still parity with the validated V5 input shape:

- `ChapterQualityGate`
- chapter selection and Book Memory roles
- target exclusion from Book Memory
- context backfill order
- `NovelCleanerModels`
- `NovelPollutionAnalyzer`
- same-book arc confirmation
- action confirmation/downgrade rules
- thresholds and confidence semantics

However, the 2026-05-23 red/green pass proved real false negatives in the
validated V5 output. Core changes are therefore allowed, but only as audited
algorithm-version changes: first against the three local red/green books, then
against the full raw corpus replay, and finally on device.

The production planner is `V5ChapterValidationPlanner`. The final 100-book
target replay used the planner constants below, so these are the production
defaults:

```text
TAIL_RISK_WINDOW_CHAPTERS = 160
TARGET_RECENT_CHAPTERS = 2
TARGET_NEIGHBOR_RADIUS = 1
TARGET_EXTENDED_MIN_OFFSET = 256
NEAR_CONTEXT_SPAN = 300
NEAR_CONTEXT_PROBE_COUNT = 8
MID_CONTEXT_PROBE_COUNT = 2
LONG_ANCHOR_PROBE_COUNT = 1
MIN_USABLE_CONTEXT_CHAPTERS = 8
MAX_CONTEXT_BACKFILL_ATTEMPTS = 256
```

Earlier `50/50` source-experiment defaults are not the production V5 validation
plan.

For large books, the production planner now targets every chapter in the last
160-chapter tail-risk window. This replaced the older 100-chapter cap after
`元始法则` proved that bad tail content can start before the former window.
Coarse extended offsets are preserved for older pollution. Short books keep
sparse tail probing when otherwise there is not enough non-target Book Memory.

## Tier, Waterfall, And V5

Ownership stays separated:

- Tier decides which source/book pairs are worth trying first.
- Waterfall decides how to continue when a source cannot satisfy a chapter.
- V5 decides whether fetched content belongs to the book/chapter.

V5 runs source-locally. There is no shared multi-source V5 report. This is
intentional because Book Memory and target evidence are source-local. Reader
scheduling validates only the best/primary trusted source by default.

When the primary source receives bad marks, the reader may compare the primary
marked chapter neighborhood against other trusted sources. A second source is
validated only when:

- the catalog can match the marked chapter neighborhood;
- at least two primary/candidate chapter text pairs are available;
- all compared text is clearly dissimilar by `V5SourceTextSimilarity`.

If the text is similar, the second source is assumed to share the same upstream
content and V5 is not re-run. This keeps multi-source cost conservative.
When a similar trusted source has cached marks and the visible anchor catalog is
from another source, marks may be projected to the visible catalog only by exact
chapter-title match under the same book name and author. They are not projected
across sources by ordinal/index alone.

Source-level V5 results then feed `SourceQualityRouter`, which records valid
latest ordinal, bad tail start, and mark counters for the per-book tier.

If source A has more raw chapters but V5 marks its tail as bad, and source B has
fewer raw chapters but more valid latest chapters, routing can promote B for
this book. The visible catalog is still changed only at an epoch boundary, not
row by row while the user is reading.

## Stability Rules

- Current chapter, first chapter, and prefetch requests keep priority over V5.
- V5 epochs are bounded to one concurrent source validation in the reader to
  avoid memory spikes from large books.
- V5 epoch wall-clock timeout is deliberately high because production keeps the
  dense validated tail plan and network fetches can be much slower than local
  raw-corpus replay.
- Successful V5 marks are persisted by source/book/catalog shape. Reopening the
  same catalog restores marks immediately instead of recomputing V5.
- V5 intermediate analyzer artifacts are not persisted yet. They are more
  tightly coupled to algorithm internals than final marks and are invalidated by
  small core changes.
- A source epoch commits once. The catalog adapter refreshes marks quietly and
  must not change the current reading position.
- Current-chapter content fallback is bounded. Quality/coherence diagnostics do
  not force fallback by themselves. Direct/tier/candidate fallback continues
  only when there is no displayable cleaned text or when source/chapter
  resolution fails. If hard failures exhaust the path, the reader records a
  request failure and enters error state instead of retrying the same
  source-engine terminal failure.
- If usable Book Memory is below 8 chapters, that source epoch is failed as
  `insufficient_book_memory` and can be retried later.

## Diagnostics Contract

High-signal events expected in runtime validation:

```text
source_catalog_anchor_displayed
source_read_catalog_applied
source_content_tier_persisted
source_catalog_v5_epoch_started
source_catalog_v5_epoch_running
source_catalog_v5_probe_fetched
source_catalog_v5_probe_fetch_failed
source_catalog_v5_diagnostic
source_catalog_v5_epoch_committed
source_catalog_v5_cache_saved
source_catalog_v5_cache_hit
source_catalog_v5_secondary_similarity
source_catalog_v5_secondary_skipped
source_content_request_failed
source_content_direct_quality_diagnostic
source_content_tier_quality_diagnostic
source_content_candidate_quality_diagnostic
source_content_direct_trusted
source_content_tier_trusted
source_content_candidate_trusted
source_content_global_refresh_skipped
source_read_chapter_retry_exhausted
source_content_load_failed
source_content_load_repeated_failed
source_read_catalog_marks_applied
source_read_catalog_marks_seen
source_catalog_wrong_toggle_changed
source_quality_v5_marks
```

Search/detail should not emit V5 epoch events. Reading should.

## Acceptance Criteria

- Opening a source-engine book shows the catalog before V5 completes.
- The catalog no longer displays `目录加载中` while waiting for two trusted
  sources.
- Current chapter loading starts before V5 background probes.
- V5 uses the final 100-book planner and the migrated analyzer core.
- V5 marks wrong/non-story/bad-extraction chapters without deleting rows.
- Low quality/coherence current-chapter content with non-empty cleaned text is
  displayed and logged as diagnostic evidence, not blocked as a loading failure.
- Reopening an unchanged catalog restores persisted V5 marks without rerunning
  the analyzer.
- `显示错章` filters the adapter view without mutating the stored catalog.
- Source-quality routing receives V5 evidence for per-book tier promotion and
  penalties.
- A second source V5 run appears only after a clearly dissimilar text-similarity
  decision.
- AI Bridge validation shows reading V5 epochs and no search/detail V5 epochs.

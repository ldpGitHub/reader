# Reading V5 Integration Technical Notes

Date: 2026-05-23

## Code Map

V5 core and production wrapper:

- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/NovelPollutionAnalyzer.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/ChapterQualityGate.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/NovelCleanerModels.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/V5ChapterValidationPlanner.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/V5SourceChapterValidator.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/V5TailContiguousPollutionExpander.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/V5SourceTextSimilarity.kt`
- `source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v5/V5Diagnostics.kt`

Reader integration:

- `app/src/main/java/com/ldp/reader/source/SourceEngineReaderContentProvider.kt`
- `app/src/main/java/com/ldp/reader/source/BookContentProviderRouter.kt`
- `app/src/main/java/com/ldp/reader/ui/activity/ReadViewModel.kt`
- `app/src/main/java/com/ldp/reader/source/SourceQualityRouter.kt`
- `app/src/main/java/com/ldp/reader/source/SourceEngineV5MarkCache.kt`

Catalog mark UI:

- `app/src/main/java/com/ldp/reader/source/SourceEngineCatalogMarkRegistry.kt`
- `app/src/main/java/com/ldp/reader/model/bean/BookChapterBean.kt`
- `app/src/main/java/com/ldp/reader/widget/page/TxtChapter.kt`
- `app/src/main/java/com/ldp/reader/widget/page/NetPageLoader.kt`
- `app/src/main/java/com/ldp/reader/ui/activity/ReadActivity.kt`
- `app/src/main/java/com/ldp/reader/ui/adapter/CategoryAdapter.kt`
- `app/src/main/java/com/ldp/reader/ui/adapter/view/CategoryHolder.kt`
- `app/src/main/res/layout/activity_read.xml`

Replay callers:

- `algorithm-test/src/test/kotlin/com/ldp/reader/algorithmtest/core/RawCorpusTargetReplayTest.kt`
- `algorithm-test/src/main/java/com/ldp/reader/algorithmtest/MainActivity.kt`

## V5 Input Boundary

Production fetches each V5 probe with source-engine content loading, but passes
the raw chapter text into V5:

```text
loadCleanContentWithTimeout(...)
  -> CleanContent.rawContent
  -> V5SourceChapterValidator
  -> ChapterQualityGate / NovelPollutionAnalyzer
```

This preserves the validated V5 responsibility boundary: source-engine may fetch
and time out network work, but V5 owns quality gating, cleaning semantics, Book
Memory, target checking, and confirmation logic.

## Validation Planner

`V5ChapterValidationPlanner.selectChapters(...)` is the single production and
replay planner. It selects:

- every target chapter in the final 160-chapter tail-risk window for large
  books;
- the last two recent target chapters;
- target neighbors;
- long/mid/near Book Memory context;
- context backfill when usable context is below 8.

Short books keep sparse tail probing when the full tail window would leave too
little non-target context.

The planner emits `v5.plan.*` diagnostics. A successful `叩问仙道` runtime pass
should match the final replay shape:

After the `元始法则` miss, the old 100-chapter cap was replaced by the
160-chapter tail window and the cache schema was bumped. The 2026-05-24
tail-160 smoke replay for `叩问仙道` has this shape:

```text
analysis=184
target=164
context=20
usableContext=20
```

## Reader Scheduling

`BookContentProviderRouter.prepareBookContentTier(..., triggerV5 = false)` keeps
search/detail behavior unchanged. `ReadViewModel.startReadingContentTierFill()`
passes `triggerV5 = true`, so reading is the V5 owner. It starts as soon as
cached or bootstrap catalog chapters are available, before the slower full
catalog refresh completes.

V5 can be scheduled from two reading paths:

- after reading-tier preparation starts: a persisted trusted source can be
  validated immediately when already resolved; otherwise V5 is scheduled as
  soon as tier fill promotes the first trusted resolved source, while candidate
  refresh and the rest of tier fill continue;
- after current chapter content resolution: the selected best source is eligible
  for validation once enough catalog/context can be fetched.

`SourceEngineReaderContentProvider` de-duplicates source epochs by
source/book key and uses one reader semaphore for validation:

```text
V5_VALIDATION_MAX_CONCURRENT_EPOCHS = 1
V5_VALIDATION_TOTAL_TIMEOUT_MS = 1_800_000
```

This bound is important for large books. It prevents concurrent V5 analyzer runs
from holding several full raw-corpus windows in memory at once. The total epoch
timeout is intentionally much higher than catalog-tail probing because
production V5 keeps the validated dense tail plan and may need to fetch 160+
chapters from real network sources before it can save marks.

Production validation does not retain per-chunk score reports after V5 finishes.
`V5SourceRunRequest.retainReportChunkScores` defaults to `false`; the analyzer
still computes the same chunks, fingerprints, suggestions, and marks, but the
large `chunkScores` debug payload is dropped before the result is returned to
the reader. The Android app also requests `largeHeap` because tail-160 epochs are
real on-device analysis workloads.

By default the reader does not run V5 over every trusted source. After the
primary source has bad marks, `V5SourceTextSimilarity` compares the marked
chapter neighborhood (`anchor ± 2`) with at most two other trusted sources. A
secondary V5 epoch is scheduled only when at least two matched text pairs are
available and both max/average shingle similarity stay below the conservative
thresholds.

## Result Cache

`SourceEngineV5MarkCache` persists final `V5ChapterMarkResult` lists under the
book cache root. The cache key includes:

- source URL and book URL;
- book name and author;
- catalog size;
- first and last catalog title;
- a digest of the last 160 catalog titles;
- cache schema version (`3` for the tail-160 plan).

Cache hits re-record marks into `SourceEngineCatalogMarkRegistry`, so the
catalog can show marks immediately after reopening. Cache hits do not re-apply
source-quality scoring; scoring is applied when the V5 epoch originally
computes and saves the result.

Only final marks are cached. Analyzer internals and intermediate feature graphs
are intentionally not cached yet.

## Mark Application

`SourceEngineCatalogMarkRegistry` stores marks by:

- exact `sourceUrl + bookUrl`;
- same-source normalized `bookName + author` as a fallback when a resolved
  source book URL and displayed chapter book URL differ.
- cross-source normalized `bookName + author + chapterTitle` as a conservative
  UI fallback when the V5 source and the current anchor catalog are different
  but have the same chapter title.

The cross-source fallback is title-scoped. It never applies a mark to a
different source by chapter index alone, because source catalogs can be shifted
or padded differently.

The registry applies marks to both `BookChapterBean` and `TxtChapter`, and
reports how many current rows match a stored mark. This feeds runtime logs:

- `source_read_catalog_apply_started` includes `markChanged` and `markMatched`.
- `source_read_catalog_marks_applied` includes `changed` and `matched`.
- `source_read_catalog_marks_seen` proves an update arrived even when no rows
  changed, and distinguishes already-applied marks from unmatched updates.

## Source Quality Evidence

`SourceQualityRouter.recordV5SourceRun(...)` and
`recordV5ChapterMarks(...)` receive per-source V5 results. The router stores
latest observed ordinal, latest normal ordinal, first bad tail ordinal, and mark
counts. The evidence affects only local book/source score deltas and personal
tier order. It does not rewrite the global seed tier from a single book.

## Test Coverage

Focused tests:

```powershell
.\gradlew.bat :source-engine:test --tests "com.ldp.reader.sourceengine.content.v5.*"
.\gradlew.bat :algorithm-test:testDebugUnitTest --tests "com.ldp.reader.algorithmtest.core.RawCorpusTargetReplayTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.ldp.reader.source.SourceQualityRouterTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.ldp.reader.source.SourceEngineCatalogMarkRegistryTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.ldp.reader.source.SourceEngineV5MarkCacheTest"
```

The registry test covers:

- exact source/book key mark application;
- same-source book identity fallback;
- cross-source title fallback;
- no cross-source index-only fallback when the title differs.

The cache test covers:

- save/load for matching catalog identity;
- invalidation when catalog size or last title changes.

The full raw-corpus target replay needs a larger test worker heap after the
tail target window expanded to 160 chapters. `algorithm-test` honors
`-DrawCorpusTargetMaxHeap=4096m` only when `-DrawCorpusTargetReplay=true`.

Full debug build:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:assembleDebug --no-parallel
```

Use `--no-parallel` for this project because ObjectBox generated sources can
race in parallel Gradle execution.

## Runtime Validation Target

Recommended AI Bridge book:

```text
叩问仙道 / 雨打青石
```

Why this book is useful:

- it is in the final 100-book replay set;
- tail-160 replay used `analysis=184`, `target=164`, `context=20`,
  `usableContext=20`;
- final replay found polluted tail chapters around the latest window;
- the app can compare production source behavior against a known V5 shape while
  accepting source-content differences.

Expected reading evidence:

```text
source_catalog_anchor_displayed ... mode_anchor-fast ... reason_single_anchor_catalog
source_read_catalog_applied ... chapters_2722
source_content_tier_persisted ... trusted_5
source_catalog_v5_epoch_running ... maxConcurrent_1
v5.plan.finish ... analysis_124 ... usableContext_20
source_catalog_v5_epoch_committed ... wrong_6 ...
source_catalog_v5_cache_saved ... saved_true ...
source_catalog_v5_cache_hit ... marks_124 ... (on reopen with unchanged catalog)
source_read_catalog_marks_applied or source_read_catalog_marks_seen
```

Expected non-reading evidence:

```text
Search page: no source_catalog_v5_epoch_* events
Detail page: no source_catalog_v5_epoch_* events
```

## Drift Check

The production result does not need to match every 100-book replay mark exactly,
because live reading uses current network sources rather than the frozen raw
corpus. The no-drift check is:

- same planner shape for the known replay book;
- same analyzer/quality-gate code path;
- same target/context split;
- same Book Memory size threshold;
- tail pollution is marked, not cropped;
- source-specific differences appear as different mark states only when live
  content differs.

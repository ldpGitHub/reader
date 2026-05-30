# Reading V8 PS-BMT Progress

Date: 2026-05-30

## Current Status

V8 is implemented as the source-engine production catalog mark detector.

Implemented:

- V8 detector, semantic model, quality gate, diagnostics, planner, source
  similarity, mark models, and source runner under `content/v8`;
- Android BGE provider and packaged `bge-small-zh-v1.5-onnx` assets;
- V8-named app scheduler, cache, tracker, secondary probe policy, catalog mark
  registry, source-quality routing, and UI mark path;
- schema 21 persisted marks;
- content-digest cache invalidation;
- stable NORMAL probe result caching, so unchanged clean books do not keep
  regenerating V8 marks;
- bad-tail continuation gate: after a confirmed bad tail, a future-rescued low
  membership chapter or tail-cluster suspect cannot reopen a visible NORMAL
  hole;
- dynamic initial and expanded target planning;
- foreground network priority gate with V8 background resumption;
- continuous low-priority shelf maintenance that sweeps every source-engine
  shelf book instead of stopping after a fixed small batch;
- constructed-sample, detector, cache, router, and provider regression tests.

Removed:

- obsolete detector packages from source and test code;
- obsolete detector design/progress documents;
- stale cache namespace used by the previous production mark path.

## Key Fixes In This Iteration

1. Expanded V8 validation now includes every tail chapter from the earliest
   abnormal backtrack to the end. It no longer filters expanded work through the
   initial target set.
2. Future rescue uses future chapter membership plus suffix-to-future
   acceptance. If the current candidate is an early fragment rupture and the
   future chapter also has early fragment risk, the future cannot rescue it;
   this catches consecutive polluted tails such as `仙人消失之后` 第2881章 ->
   第2882章.
3. V8 cache keys include fetched body content digest. Catalog-title equality
   alone cannot reuse old marks.
4. V8 app classes and events now use V8 names so production code does not imply
   another detector line is active.
5. Real BGE target validation now uses manual first-wrong boundaries instead of
   old labels, and parses both `章` and `节` titles. Sparse cache rows without
   nearby previous chapters fall back to `INSUFFICIENT_CONTEXT` instead of using
   distant chapters as fake reference context.
6. Low-priority maintenance no longer caps a pass to four books. It sweeps all
   source-engine shelf books, prepares only the dedicated readable tier needed
   to trigger V8, and then moves on instead of blocking on global low-priority
   tier filling. Books that are not ready, including timed-out books, retry
   after the short idle delay. A clean pass sleeps until the normal 15-minute
   trigger; unchanged catalogs/content hit schema-21 + content-digest cache
   instead of recalculating V8 marks.
7. Clean-result caching also survives a thin `INCONCLUSIVE` probe when enough
   stable `NORMAL` marks remain after dropping the fragile thin chapter. This
   prevents a mostly-clean book from rerunning every maintenance cycle because
   one empty/too-short chapter was intentionally not trusted.
8. Source-engine readable-body cache is bumped to `source-engine-content-v9` so
   stale per-title `.nb` bodies from older source choices cannot be mistaken for
   current schema-21 verification evidence.

## Known Real-Book Finding

`清光宝鉴` had intermittent missed marks after the first bad chapter. Manual
inspection of cached chapter bodies showed that several gap chapters were real
pollution cases with the same early prefix-then-foreign-tail shape.

Observed bad gap examples:

```text
第七十九章：朱棺鬼咒
第八十六章：广寒来仙子、莲中五色光！(1.0798w!)
第八十七章：太古道场、酒中道妙！(9.437k！)
第八十八章：酒仙人的酒、茶仙子的茶
第八十九章：剑窟狂剑、劫仙法力!(9.069k！)
第九十二章：道花华池开，乘月醉高台！
第九十四章：一界大能、往事越千年!
```

The expected V8 behavior after this iteration is that those chapters enter the
expanded V8 run and are individually judged, rather than being skipped by the
planner.

## Validation Commands

Detector and source-engine tests:

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Users\ldp\.jdks\corretto-17.0.18" :source-engine:test --tests com.ldp.reader.sourceengine.content.v8.* --no-daemon
```

App V8 regression tests:

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Users\ldp\.jdks\corretto-17.0.18" :app:testDebugUnitTest --tests com.ldp.reader.source.SourceEngineV8MarkCacheTest --tests com.ldp.reader.source.SourceEngineV8SecondaryProbePolicyTest --tests com.ldp.reader.source.SourceEngineV8ValidationTrackerTest --tests com.ldp.reader.source.SourceQualityRouterTest --no-daemon
```

Build:

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Users\ldp\.jdks\corretto-17.0.18" :app:assembleDebug --no-daemon
```

Real BGE target-book validation:

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Users\ldp\.jdks\corretto-17.0.18" -Dv8BgeTargetValidation=true :source-engine:test --tests com.ldp.reader.sourceengine.content.v8.V8PsbmtDetectorTest.reportsBgeTargetBooksValidationWhenEnabled --no-daemon
```

Latest result:

```text
records=112
normal=98
polluted=14
normalWrong=0
normalWrongOrSuspect=0
pollutedCaught=14
medianMs=1051
p90Ms=1596
maxMs=2297
```

Device validation uses AI Bridge after installing the debug APK. Required
runtime checks:

```text
open shelf books
trigger V8 validation
pull source_engine_v8_marks cache
verify schema 21 and content digest
verify first wrong chapter and +/- 1..2 neighbors
verify gap chapters after first wrong are not skipped
```

## Books To Verify On Device

Shelf verification must cover:

```text
青山
清光宝鉴
叩问仙道
仙都
元始法则
苟在武道世界成圣
仙人消失之后
灵源仙途：我养的灵兽太懂感恩了
苟在两界修仙
我在修仙界万古长青
```

For any book with a first wrong mark, manually inspect the first wrong chapter
and the one or two adjacent chapters before and after it.

## Current Acceptance Target

V8 is considered complete for this task only after:

```text
source and app tests pass
debug APK builds
AI Bridge device run writes current V8 marks for the shelf
清光宝鉴 intermittent gap chapters are rechecked
no correct adjacent chapter is marked WRONG
no known polluted adjacent/gap chapter remains NORMAL because it was skipped
```

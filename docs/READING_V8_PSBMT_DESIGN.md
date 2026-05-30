# Reading V8 PS-BMT Design

Date: 2026-05-30

## Decision

V8 is the production chapter-integrity detector for source-engine reading
catalog marks.

It answers one question:

```text
Does the current chapter start as this book, then enter a sustained foreign
fragment tail without trusted future acceptance?
```

The implementation lives under:

```text
source-engine/src/main/kotlin/com/ldp/reader/sourceengine/content/v8/
```

App integration uses V8-named scheduler, cache, provider, routing, and UI mark
classes. Old detector packages are removed from source and test code.

## Runtime Shape

```text
open reader/catalog
  -> build dynamic V8 validation plan
  -> fetch target bodies plus nearby context at background priority
  -> run V8SourceChapterValidator
  -> persist V8 marks behind schema 21 and content digest
  -> update catalog mark registry and source-quality routing
```

Foreground chapter/search/detail network work keeps priority. V8 background work
uses the network gate and resumes when foreground requests drain.

## Dynamic Plan

The first pass is intentionally small:

```text
tail targets: last 16 markable chapters
tail anchors: 24, 32, 48, and 64 chapters from the end
context: nearest trusted previous and future chapters required by PS-BMT
```

If any target is not `NORMAL`, V8 expands from before the earliest abnormal
target through the catalog tail. This expansion is verification behavior, not a
blind tail-propagation rule. Every marked chapter must still be individually
validated by V8.

After a bad tail is observed, `V8SourceChapterValidator` keeps the sequence
coherent: a low-membership chapter that is only saved by future rescue, or a
tail-cluster suspect, is marked as tail continuation instead of creating a
NORMAL gap. This gate only activates after V8 has already seen a bad tail in the
same run.

## Cache Contract

V8 cache hits require:

```text
schema version matches
source identity matches
book identity matches
catalog shape matches
target chapter indexes match exactly
content digest matches exactly
```

The content digest is an MD5 over the fetched V8 input bodies:

```text
target indexes
input chapter index
input title
input content length
input content
```

Catalog title identity alone is not enough. If a source silently updates body
text, the digest changes and V8 reruns.

## Clean Layer

The clean layer only prepares text:

- remove HTML tags and simple entities;
- remove URL/navigation/app/bookmark/vote lines;
- collapse repeated adjacent lines;
- remove a duplicated chapter title at the beginning.

It does not mark a story chapter wrong. Too little usable body returns
`SOURCE_QUALITY_PROBLEM`; insufficient trusted previous context returns
`INSUFFICIENT_CONTEXT`.

## Semantic Membership

V8 supports two semantic implementations behind the same interface:

- `V8BgeSemanticModel`: Android production path backed by packaged BGE ONNX
  assets.
- `V8SparseSemanticModel`: JVM test path using sparse character n-gram vectors.

Reference text comes only from trusted previous chapters. Future chapters are
never added to the book reference because the future may already be polluted.

Reference defaults:

```text
window size: 192 chars
window stride: 192 chars
max previous chapters: 3
max chars per previous chapter: 2600
```

## Identity Sketch

Identity sketch is a weighted local Chinese n-gram fingerprint built from
trusted previous chapters. It is not a dictionary and it does not classify
people, places, factions, realms, or items.

The sketch is auxiliary evidence. It can support semantic membership, but it
cannot convict a chapter by itself.

## Candidate Scan

Candidates are scanned near the observed prefix-tail break region, plus a small
sparse extension:

```text
dense: 64..260 step 8
sparse: 284..min(800, current.length * 0.55) step 24
local rupture cue max offset: 320
```

Position is a candidate prior only. A chapter is marked wrong only when the text
evidence is strong.

## Main Evidence

For each candidate offset V8 computes:

```text
prefixSupport
suffixSupportMedian
suffixLowRatio
belongDrop
localRupture
suffixRepeatRatio
futureTrust
futureSupport
futureFragmentRisk
tailRisk
localRuptureCue
```

`localRuptureCue` is an auxiliary candidate signal for the observed source
pollution pattern. It requires an early offset, enough suffix text, low suffix
internal repeated n-gram continuity, and a strong local rupture.

Future rescue is based on future chapter membership and suffix-to-future
acceptance. If the current candidate is an early fragment rupture and the
future chapter also has early fragment risk, the future is not allowed to rescue
the current suffix. This prevents consecutive polluted tails from washing each
other back to `NORMAL`.

## Decision Contract

`WRONG_CONFIRMED` requires all of:

```text
prefix belongs to this book
suffix membership is absolutely and persistently low, or the chapter has a
  bounded early fragment rupture matching the observed source-pollution shape
no trusted future rescue
no tail-cluster explanation
not suffix-safe
```

`SUSPECT_RECHECK_REQUIRED` is used for:

```text
tail cluster risk
medium early prefix-suffix evidence
fragment evidence below confirmed confidence
whole-chapter low-support cases that need later source comparison
```

`NORMAL` requires the absence of a strong candidate, or a trusted future that
accepts the suffix as continuing this book.

## Pseudocode

```kotlin
fun detect(input: V8Input): V8Decision {
    val current = clean(input.current)
    val previous = cleanTrustedPrevious(input.previous)
    val future = cleanFuture(input.future)

    if (!hasEnoughCurrent(current)) return sourceQualityProblem()
    if (!hasEnoughPrevious(previous)) return insufficientContext()

    val semanticSpace = semanticModel.build(previous, current, future)
    val identity = buildIdentitySketch(previous)
    val calibration = calibrate(previous, semanticSpace, identity)
    val futureEvidence = evaluateFuture(future, semanticSpace, identity, calibration)

    val candidates = scanOffsets(current).map { offset ->
        scoreCandidate(
            offset = offset,
            current = current,
            semanticSpace = semanticSpace,
            identity = identity,
            calibration = calibration,
            future = futureEvidence
        )
    }

    return decide(candidates, futureEvidence)
}
```

## Validation Gate

V8 is accepted only when all of these pass:

```text
constructed pollution fixtures: high recall and cut-point tolerance
normal chapter fixtures: no WRONG and no broad SUSPECT spread
real target books: first wrong chapter and neighbors manually checked
device run: V8 cache files created with current schema and digest
performance: background work does not block foreground reading
```

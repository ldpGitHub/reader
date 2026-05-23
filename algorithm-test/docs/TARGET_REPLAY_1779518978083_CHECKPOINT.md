# Target Replay 1779518978083 Checkpoint

Date: 2026-05-23

This checkpoint records the validated V5 replay node for the 100-book-scale corpus.
The replay actually contains 101 books.

## Replay Result

- Replay output: `algorithm-test/algorithm-test/build/raw-corpus-target-replay-1779518978083`
- Summary copied to: `algorithm-test/test-datasets/raw-corpus-101-bundle/manifests/latest-target-replay-summary.tsv`
- Books: 101
- Books with suggestions: 29
- Suggestions: 110
- Baseline output diff: `removed=0`, `added=0`

## Verification Boundary

- The full downloaded novel corpus is kept as local test data only.
- The local backup path is `algorithm-test/test-datasets/raw-corpus-101-bundle/raw-corpus-101`.
- Raw novel chapter text, replay detail directories, and fixture snapshots under that bundle are ignored by Git.
- Only lightweight metadata, docs, and manifests are intended for commit.

## Manual Audit Scope

The replay was checked at the user-experience and algorithm-output level after the V5 hard-boundary fixes.
The audited point is the output set stability against the accepted baseline:

- `books=101`
- `removed=0`
- `added=0`

This checkpoint is suitable as the current regression baseline before further speed or algorithm-structure experiments.

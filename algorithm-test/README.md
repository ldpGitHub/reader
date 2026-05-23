# Algorithm Test Module

`algorithm-test` is an isolated Android library module for novel fingerprint,
catalog fusion, and pollution-detection algorithms.

Rules for this module:

- Keep all experimental algorithm code inside `algorithm-test`.
- Do not modify the production reader modules while experimenting.
- Log every important step with `AlgorithmTrace`.
- Prefer measurable reports over hidden decisions.

Current MVP:

- Open `com.ldp.reader.algorithmtest.MainActivity` from the debug host app.
- Paste chapter text and run local pollution detection.
- Paste a Legado-compatible source JSON and fetch/search/catalog/content samples.
- Compare catalog candidates and run tail-chapter analysis on fetched chapters.
- `Run Bundled Sources` also loads the host app's source-quality seed, so the
  phone-side waterfall tests verified/tiered sources before raw JSON order.
- Successful and failed phone-side runs write reports under the app external
  files `algorithm-test` directory. Pull these files before judging tail
  chapters manually.
- Batch source validation schedules the whole configured target list together
  with bounded phone-side parallelism. The current debug harness caps active
  book experiments at 8 and imports the bundled source JSON once for the whole
  batch.

Manual real-novel probe:

```powershell
.\gradlew.bat :algorithm-test:testDebugUnitTest `
  --tests com.ldp.reader.algorithmtest.source.RealNovelProbeTest `
  -DrealNovelProbe=true `
  -DrealNovelTitle=仙逆 `
  -DrealNovelAuthor=耳根
```

The probe writes the algorithm report and fetched real chapter text to
`algorithm-test/build/reports/real-novel-probe/`. This output is for manual
validation; it is not a default unit test.

Pasted manual chapters use this format:

```text
### Chapter title
Chapter content...

### Next chapter
Chapter content...
```

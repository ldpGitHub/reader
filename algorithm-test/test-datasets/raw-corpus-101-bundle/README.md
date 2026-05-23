# Raw Corpus 101 Test Bundle

This directory is a test-only data bundle for the algorithm-test module.

It is intentionally outside Gradle `build/` so the downloaded corpus is not lost
when app data is cleared, the app is uninstalled, or Gradle build outputs are
cleaned.

## Contents

- `raw-corpus-101/`: copied raw downloaded corpus from the phone-side fetch run.
- `results/`: replay outputs and summaries used as red/green references.
- `fixtures/`: unit-test fixture snapshot references.
- `manifests/`: generated file counts, sizes, and copy notes.

## Copy To Phone

```powershell
adb push algorithm-test/test-datasets/raw-corpus-101-bundle/raw-corpus-101 /sdcard/Download/reader-test-data/
```

Default device directory:

```text
/sdcard/Download/reader-test-data/raw-corpus-101
```

The targeted replay root on device should point at:

```text
/sdcard/Download/reader-test-data/raw-corpus-101/device-full/extracted-wsl/fetch-batch-1779484863140
```

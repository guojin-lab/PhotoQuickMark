# Verification and limits

## Historical local evidence (2026-09-14, v0.2.1)

The maintainer's original project includes JUnit XML reports with 11 geometry tests and 5 label-placement tests, all passing, plus build/lint/device records. Reports contain local machine metadata and are not republished verbatim.

The original verification notes record successful debug/test APK builds, JVM tests and lint. Six device test cases reported success individually, but the ADB connection dropped before the overall runner completed (exit code 1). This is **not** a clean end-to-end instrumentation run.

The README now shows current v1.0.0 screenshots captured by hand on the test phone; they document the interface only and are not evidence of an instrumentation run. The earlier v0.1.0 synthetic capture referenced above has been replaced.

## Fresh public-repository checks

See the [Actions runs](https://github.com/guojin-lab/PhotoQuickMark/actions) for fresh results tied to an exact commit. CI checks unit tests, Android lint, debug assembly and instrumentation-test compilation. It does **not** run device tests or establish human usability.

## Device and load tests

Use a test device without valuable project data. Normal instrumentation:

```sh
./gradlew connectedDebugAndroidTest
```

The load test generates 100 synthetic 4000×3000 JPEGs with 100 annotations each and requires explicit opt-in, after installing both app and test APK:

```sh
adb shell am instrument -w -r -e class com.guojinlab.photoquickmark.LoadTest -e load true com.guojinlab.photoquickmark.test/androidx.test.runner.AndroidJUnitRunner
```

It consumes storage and memory. Historical v0.1.0 load results do not establish performance on other devices or current code. Large PDFs downsample photos; exported image files retain a separate resolution policy.

## Outstanding acceptance

- Complete camera/Photo Picker/export destination and denied-permission flows.
- Manual coverage for the per-line arrow and label-size sliders: the capsule must re-measure at the selected size, keep a constant on-screen size while pinching, and survive undo, export and reopening the photo.
- Human gesture testing, accessibility, landscape, larger fonts and screen sizes.
- Kill/restart recovery, storage exhaustion and interrupted-export fault injection.
- Older/low-memory device coverage and database upgrade migrations.
- Current UI screenshots and complete device test runs without connection failure.

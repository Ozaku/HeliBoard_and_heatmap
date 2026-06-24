# Native code and build

## Gradle modules

`settings.gradle`:

```
include ':app'
include ':tools:make-emoji-keys'
```

## App build (`app/build.gradle.kts`)

| Setting | Value |
|---------|-------|
| `applicationId` | `helium314.keyboard` |
| `minSdk` | 21 |
| `targetSdk` / `compileSdk` | 36 |
| NDK ABIs | armeabi-v7a, arm64-v8a, x86, x86_64 |

### Build types

| Variant | Notes |
|---------|-------|
| `release` | Minify on |
| `nouserlib` | Like release; **disallows** user-supplied `libjni_latinime.so` |
| `debug` | Minify on (smaller APK); `applicationIdSuffix = .debug`; strips `main_ro.dict` from assets |
| `debugNoMinify` | Faster IDE builds; debuggable |
| `runTests` | CI tests; no minify |

APK name pattern: `HeliBoard_{versionName}-{buildType}.apk`

## Native library

- **Built as:** `libjni_latinime.so` via `ndk-build` / `Android.mk` under `app/src/main/jni/`
- **Static core:** `libjni_latinime_common_static` linked into shared lib
- **JNI_OnLoad:** `jni_common.cpp` registers `BinaryDictionary`, `ProximityInfo`, `DicTraverseSession`, etc.

### JNI bridge files (examples)

- `com_android_inputmethod_latin_BinaryDictionary.cpp` — `getSuggestions`, updates
- Proximity and traverse session companions in same tree

### Suggest subsystem (native)

```
jni/src/suggest/
├── core/suggest.cpp              # Main suggestion loop
├── core/layout/proximity_info.cpp
├── policyimpl/typing/            # Tap typing traversal + scoring
├── policyimpl/gesture/           # Gesture policy factory (stub in OSS)
└── core/result/                  # Output formatting
```

## Library load order (`JniUtils.java`)

1. **User import:** `{filesDir}/libjni_latinime.so` if checksum matches prefs / expected bundled hash → `sHaveGestureLib = true`
2. **Google:** `System.loadLibrary("jni_latinimegoogle")` if available on device
3. **Bundled:** `System.loadLibrary("jni_latinime")` — always available in APK

Checksums hardcoded per ABI in `JniUtils` for verifying user libs match expected binary.

Build type `nouserlib` skips user library path entirely.

## Java package vs native

JNI method names follow `com.android.inputmethod.*` classes even though HeliBoard code lives under `helium314.keyboard.*`. Changing package names **breaks JNI** unless native registrations updated.

## Tools module

`:tools:make-emoji-keys` — Kotlin JVM, generates emoji keyboard resources from Unicode data; not part of IME runtime decode path.

## Testing

- JVM: `app/src/test/` (e.g. `SuggestTest.kt`)
- Native: `app/src/main/jni/tests/` (linked in some CI variants)
- Android instrumented: `app/src/androidTest/`

## Refactor build considerations

- Any new native policy → update `Android.mk` / CMake if migrated
- Gesture experiments may ship as alternate `.so` until merged into main `libjni_latinime`
- Use `debugNoMinify` for fast iteration; `debug` for realistic APK size

Related: [04_swipe_gesture_input.md](04_swipe_gesture_input.md), [07_key_files_index.md](07_key_files_index.md)

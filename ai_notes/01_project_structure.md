# Project structure

Root: `c:\Users\Ozaku\Desktop\keyboard_thing\HeliBoard_and_heatmap\`

Gradle includes only two modules (`settings.gradle`):

- `:app` — Android IME application
- `:tools:make-emoji-keys` — build tool for emoji key data from Unicode emoji-test files

## Top-level layout

```
HeliBoard_and_heatmap/
├── app/                    # Main keyboard APK (IME + settings + JNI)
├── tools/
│   └── make-emoji-keys/    # Kotlin JVM tool → emoji resources
├── ai_notes/               # This documentation set (agents read first)
├── fastlane/               # Play Store metadata, changelogs
├── art/                    # Graphics / branding
├── .github/                # Issue templates, workflows
├── settings.gradle
├── build.gradle            # Root build
└── README.md               # Upstream HeliBoard readme
```

## `app/` module

```
app/
├── build.gradle.kts        # Variants, NDK ABIs, ProGuard, dict asset exclusions
├── proguard-rules.pro
├── dontoptimize.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/
    │   │   ├── helium314/keyboard/     # All HeliBoard-specific code
    │   │   └── com/android/inputmethod/ # JNI glue (BinaryDictionary, ProximityInfo, …)
    │   ├── jni/                        # libjni_latinime (latinime native)
    │   ├── assets/                     # Layouts JSON, locale key texts, dict metadata
    │   └── res/                        # Themes, strings, keyboard XML templates
    ├── test/                           # Unit tests (e.g. SuggestTest.kt)
    └── androidTest/                    # Instrumentation tests
```

## `helium314.keyboard` packages (main code)

| Package / area | Role |
|----------------|------|
| `latin/` | IME core: `LatinIME`, `InputLogic`, `Suggest`, dictionaries, spell checker |
| `keyboard/` | Views, touch tracking, layouts, emoji keyboard, internal parsers |
| `event/` | `Event`, combiner chains (Hangul, Khipro, dead keys, etc.) |
| `settings/` | Jetpack Compose settings screens and preferences |
| `accessibility/` | TalkBack / accessibility helpers |

## `com.android.inputmethod` (JNI stubs)

Small set of classes kept under AOSP package names so JNI method names match native registration:

- `latin/BinaryDictionary.java` — dictionary handle, `getSuggestions` JNI
- `keyboard/ProximityInfo.java` — keyboard geometry → native proximity grid
- Related utils under `latin/utils/`

**Do not confuse** with full AOSP tree — only stubs present here.

## `app/src/main/jni/`

Native **Latin IME** decoder (`libjni_latinime.so`):

```
jni/
├── Android.mk, Application.mk
├── jni_common.cpp              # JNI_OnLoad, class registration
├── com_android_inputmethod_latin_BinaryDictionary.cpp
├── src/
│   ├── suggest/                # Suggest loop, policies, proximity, scoring
│   ├── dictionary/             # Patricia trie, binary dict format v2/v4
│   └── utils/
└── tests/                      # Native unit tests (optional CI)
```

## `app/src/main/assets/`

- `layouts/main/*.json` — custom keyboard layouts (Floris-style parser)
- `locale_key_texts/*.txt` — per-locale key labels
- `dictionaries_in_dict_hashes.txt`, `known_dict_hashes.txt` — dictionary download/cache metadata (`.dict` binaries not always in repo)

## Supporting directories

| Path | Purpose |
|------|---------|
| `fastlane/metadata/android/` | Per-locale store listings |
| `tools/make-emoji-keys/` | Generates emoji keyboard data from UCD `emoji-test.txt` |
| `.github/` | Community templates, CI |

## What is *not* in this repo

- Full Google **swypelibs** gesture policy (optional external `.so`)
- Shipped main language `.dict` files in debug (large dict stripped — see `06_native_and_build.md`)
- Separate “heatmap” module (folder name only)

## Related docs

- Flow: [02_architecture_overview.md](02_architecture_overview.md)
- Autocorrect: [03_autocorrect_and_suggestions.md](03_autocorrect_and_suggestions.md)
- Swipe: [04_swipe_gesture_input.md](04_swipe_gesture_input.md)

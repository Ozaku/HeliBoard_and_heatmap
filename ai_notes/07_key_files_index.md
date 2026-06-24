# Key files index

Quick lookup for agents. Paths relative to repo root unless noted.

## IME core

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/latin/LatinIME.java` | `InputMethodService` entry; batch input callbacks; dict facilitator lifecycle |
| `app/src/main/java/helium314/keyboard/latin/inputlogic/InputLogic.java` | Input state machine, commit, composing, suggestion application |
| `app/src/main/java/helium314/keyboard/latin/inputlogic/InputLogicHandler.java` | Async suggestion requests (tap + gesture) |
| `app/src/main/java/helium314/keyboard/latin/Suggest.kt` | Suggestion fetch; autocorrect policy; batch vs tap |
| `app/src/main/java/helium314/keyboard/latin/WordComposer.java` | Composing word, batch mode, pointers |
| `app/src/main/java/helium314/keyboard/latin/RichInputConnection.java` | `InputConnection` wrapper, composing text |
| `app/src/main/java/helium314/keyboard/latin/NgramContext.java` | Previous-word context for predictions |
| `app/src/main/java/helium314/keyboard/latin/common/ComposedData.java` | Snapshot for native decode |
| `app/src/main/java/helium314/keyboard/latin/common/InputPointers.java` | Gesture coordinate arrays |
| `app/src/main/java/helium314/keyboard/latin/SuggestedWords.java` | Suggestion list model |
| `app/src/main/java/helium314/keyboard/latin/settings/Settings.java` | Global settings access |
| `app/src/main/java/helium314/keyboard/latin/utils/SuggestionSpanUtils.kt` | Underline / span for auto-correction |

## Dictionaries

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/latin/DictionaryFacilitatorImpl.kt` | Multi-dict load, query, merge, gesture garbage filter |
| `app/src/main/java/helium314/keyboard/latin/DictionaryFacilitator.java` | Facilitator interface |
| `app/src/main/java/helium314/keyboard/latin/dictionary/Dictionary.java` | Type constants, base class |
| `app/src/main/java/helium314/keyboard/latin/dictionary/DictionaryFactory.kt` | Construct dict instances from files |
| `app/src/main/java/com/android/inputmethod/latin/BinaryDictionary.java` | JNI dictionary API |
| `app/src/main/java/helium314/keyboard/latin/personalization/UserHistoryDictionary.java` | Learned words dict |
| `app/src/main/java/helium314/keyboard/latin/personalization/PersonalizationHelper.java` | Learning hooks |
| `app/src/main/assets/dictionaries_in_dict_hashes.txt` | Dict download metadata |
| `app/src/main/assets/known_dict_hashes.txt` | Known dict hashes |

## Autocorrect / suggestions UI

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/latin/utils/AutoCorrectionUtils.java` | Threshold checks |
| `app/src/main/java/helium314/keyboard/latin/suggestions/SuggestionStripView.kt` | Suggestion bar UI |
| `app/src/main/java/helium314/keyboard/latin/utils/SuggestionResults.kt` | Merged native results container |
| `app/src/test/java/helium314/keyboard/latin/SuggestTest.kt` | Autocorrect unit tests |

## Keyboard / touch

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/keyboard/PointerTracker.java` | Touch → key/gesture |
| `app/src/main/java/helium314/keyboard/keyboard/internal/BatchInputArbiter.java` | Gesture stroke aggregation |
| `app/src/main/java/helium314/keyboard/keyboard/internal/GestureEnabler.java` | When glide typing allowed |
| `app/src/main/java/helium314/keyboard/keyboard/internal/GestureStrokeRecognitionPoints.java` | Stroke sampling |
| `app/src/main/java/helium314/keyboard/keyboard/KeyboardActionListenerImpl.kt` | View → IME events |
| `app/src/main/java/helium314/keyboard/keyboard/MainKeyboardView.java` | Main keyboard surface |
| `app/src/main/java/helium314/keyboard/keyboard/KeyboardSwitcher.java` | Layout / mode switching |
| `app/src/main/java/com/android/inputmethod/keyboard/ProximityInfo.java` | Key geometry → native |

## JNI / native

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/latin/utils/JniUtils.java` | Load libjni_latinime / google / user |
| `app/src/main/jni/jni_common.cpp` | JNI_OnLoad |
| `app/src/main/jni/com_android_inputmethod_latin_BinaryDictionary.cpp` | Dictionary JNI |
| `app/src/main/jni/src/suggest/core/suggest.cpp` | Native suggest loop |
| `app/src/main/jni/src/suggest/core/layout/proximity_info.cpp` | Spatial proximity grid |
| `app/src/main/jni/src/suggest/policyimpl/gesture/gesture_suggest_policy_factory.cpp` | Gesture policy factory (null in OSS) |
| `app/src/main/jni/src/suggest/policyimpl/typing/typing_scoring.cpp` | Tap scoring |
| `app/src/main/jni/src/utils/autocorrection_threshold_utils.cpp` | Auto-correct confidence |
| `app/src/main/jni/Android.mk` | NDK build definition |

## Settings / gesture lib

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/settings/screens/GestureTypingScreen.kt` | Glide typing settings |
| `app/src/main/java/helium314/keyboard/settings/preferences/LoadGestureLibPreference.kt` | Import swypelibs |
| `app/src/main/java/helium314/keyboard/latin/settings/GestureDataGatheringSettings.kt` | Research data collection |

## Event / layout / misc

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/event/Event.java` | Key event model |
| `app/src/main/java/helium314/keyboard/event/HangulCombiner.kt` | Korean composition |
| `app/src/main/java/helium314/keyboard/keyboard/internal/keyboard_parser/` | JSON layout parser (Floris-derived) |
| `app/src/main/assets/layouts/main/*.json` | Custom keyboard layouts |
| `app/src/main/AndroidManifest.xml` | IME + spell checker services |
| `app/build.gradle.kts` | Variants, NDK, dict asset excludes |

## Spell checker

| Path | Purpose |
|------|---------|
| `app/src/main/java/helium314/keyboard/latin/spellcheck/AndroidSpellCheckerService.java` | System spell checker integration |

## Tools (non-IME)

| Path | Purpose |
|------|---------|
| `tools/make-emoji-keys/` | Generate emoji keys from UCD files |

## ai_notes (this folder)

| Path | Purpose |
|------|---------|
| `ai_notes/README.md` | Index + agent rules |
| `ai_notes/01_project_structure.md` | Tree and modules |
| `ai_notes/02_architecture_overview.md` | End-to-end flows |
| `ai_notes/03_autocorrect_and_suggestions.md` | Correction policy |
| `ai_notes/04_swipe_gesture_input.md` | Gesture + proximity |
| `ai_notes/05_dictionaries_and_learning.md` | Dicts + personalization |
| `ai_notes/06_native_and_build.md` | NDK + Gradle |
| `ai_notes/07_key_files_index.md` | This file |

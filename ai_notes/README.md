# ai_notes — HeliBoard refactor project

**Read this folder first.** All AI agents working on this repo should use these notes for context, direction, and file lookup before editing application code.

## Project mission

HeliBoard is an open-source Android keyboard (fork of AOSP LatinIME). The maintainer of this fork wants to **gut and rebuild** two subsystems from the ground up:

1. **Autocorrect** — suggestion selection, thresholds, when to auto-replace typed text
2. **Swipe / gesture typing** — stroke capture, spatial decoding, integration with dictionaries

**User pain (motivation):** Swipe often inserts letters from keyboard rows the finger never crossed; tap typing feels over-corrected compared to older keyboards that “just typed what you wanted.”

**Current phase:** **Coding Block 1** — beta `0.0.0.1` step 1 (WordSlot IDs). See `26_BLOCK1_INSTRUMENTATION_SUBSTEPS_v1.txt`.

## Document index

### Architecture (existing codebase)

| File | Contents |
|------|----------|
| [01_project_structure.md](01_project_structure.md) | Directory tree, modules, major folders |
| [02_architecture_overview.md](02_architecture_overview.md) | IME lifecycle, input flow, key classes |
| [03_autocorrect_and_suggestions.md](03_autocorrect_and_suggestions.md) | Suggestion pipeline, autocorrect gates, scoring |
| [04_swipe_gesture_input.md](04_swipe_gesture_input.md) | Batch input, proximity, gesture policy gap |
| [05_dictionaries_and_learning.md](05_dictionaries_and_learning.md) | Dict types, assets, personalization |
| [06_native_and_build.md](06_native_and_build.md) | JNI, NDK, Gradle variants, library loading |
| [07_key_files_index.md](07_key_files_index.md) | Quick path → purpose lookup |

### Grand plan — **v4 (read first)**

| File | Contents |
|------|----------|
| **[25_GRAND_PLAN_AND_IMPLEMENTATION_ROADMAP_v4.txt](25_GRAND_PLAN_AND_IMPLEMENTATION_ROADMAP_v4.txt)** | **Master doc: swipe vs AC split, context engine, steps 0–43** |

### Planning lockdown — v3 (superseded by v4 roadmap)

| File | Contents |
|------|----------|
| [22_PLANNING_LOCKDOWN_v3.txt](22_PLANNING_LOCKDOWN_v3.txt) | Earlier phase breakdown |
| [23_MAINTAINER_FEEDBACK_RESOLVED_v3.txt](23_MAINTAINER_FEEDBACK_RESOLVED_v3.txt) | Decisions from Composer_Suggestions # notes |
| [24_GLOSSARY_v3.txt](24_GLOSSARY_v3.txt) | Trie, IME vs spell checker, strip, slots |
| [20_FEATURE_literal_swipe_dictionary_narrowing_v3.txt](20_FEATURE_literal_swipe_dictionary_narrowing_v3.txt) | Decode spec + NEXT example; commit/strip locked |
| [Composer_Suggestions.txt](Composer_Suggestions.txt) | Original suggestions + your # feedback preserved |

### Feature plan — v2 (reference)

| File | Contents |
|------|----------|
| [Planned Features.txt](Planned%20Features.txt) | Original user prompt verbatim |
| [00_MASTER_PLAN_v2.txt](00_MASTER_PLAN_v2.txt) | Phases, compatibility strategy, terminology |
| [16_OPEN_QUESTIONS_v2.txt](16_OPEN_QUESTIONS_v2.txt) | Answered + remaining decisions |
| [17_USER_DECISIONS_AND_GLOSSARY_v2.txt](17_USER_DECISIONS_AND_GLOSSARY_v2.txt) | Hybrid swipe explained, loops explained, typo list rules |
| [20_FEATURE_literal_swipe_dictionary_narrowing_v2.txt](20_FEATURE_literal_swipe_dictionary_narrowing_v2.txt) | Literal swipe + STARTING walkthrough algorithm |
| [13_FEATURE_common_typo_list_v2.txt](13_FEATURE_common_typo_list_v2.txt) | Common Typo List (renamed), tap escape, swipe→tap fix |
| [18_SETTINGS_MENU_SPEC_v2.txt](18_SETTINGS_MENU_SPEC_v2.txt) | **Heatmap Smart Keyboard** settings group |
| [19_PERFORMANCE_AND_MEMORY_v2.txt](19_PERFORMANCE_AND_MEMORY_v2.txt) | RAM/dict mmap, caps, ML-without-AI |
| [21_AGENT_SUGGESTIONS_AND_FINAL_NOTES_v2.txt](21_AGENT_SUGGESTIONS_AND_FINAL_NOTES_v2.txt) | Short suggestions index |
| **[Composer_Suggestions.txt](Composer_Suggestions.txt)** | **Full suggestions + explanations (read next session)** |

### Feature plan — v1 (archived reference)

| File | Contents |
|------|----------|
| [00_MASTER_PLAN_v1.txt](00_MASTER_PLAN_v1.txt) | Initial phases (superseded by v2 where noted) |
| [10_FEATURE_swipe_three_signal_decoder_v1.txt](10_FEATURE_swipe_three_signal_decoder_v1.txt) | Early three-signal spec |
| [11_FEATURE_paragraph_heatmap_v1.txt](11_FEATURE_paragraph_heatmap_v1.txt) | Paragraph journal (still valid detail) |
| [12_FEATURE_local_weight_training_v1.txt](12_FEATURE_local_weight_training_v1.txt) | Weight layers (still valid detail) |
| [13_FEATURE_naughty_list_v1.txt](13_FEATURE_naughty_list_v1.txt) | **Superseded name** → see v2 common typo list |
| [14_FEATURE_dual_mode_key_geometry_v1.txt](14_FEATURE_dual_mode_key_geometry_v1.txt) | Tap/swipe geometry (see v2 settings toggles) |
| [15_IMPLEMENTATION_GUIDE_swipe_and_scoring_v1.txt](15_IMPLEMENTATION_GUIDE_swipe_and_scoring_v1.txt) | Legacy HeliBoard swipe path |
| [16_OPEN_QUESTIONS_v1.txt](16_OPEN_QUESTIONS_v1.txt) | Original question set |

## Rules for agents

1. **No application code edits** unless the user explicitly asks (Java/Kotlin/C++, layouts, JNI under `app/`).
2. **Update these notes** when you discover architectural facts or complete refactor milestones; keep cross-links accurate.
3. **Package split:** `helium314.keyboard.*` = HeliBoard UI and logic; `com.android.inputmethod.*` = thin JNI-facing stubs (AOSP legacy package names).
4. **Native is authoritative** for fuzzy match scores and spatial (proximity) geometry; Kotlin/Java applies policy on top.
5. **Gesture decode policy** is not fully open-source in-tree — see [04_swipe_gesture_input.md](04_swipe_gesture_input.md).
6. **Repo name** includes `heatmap` but no heatmap-specific implementation was found at documentation time.
7. **User file versioning rule:** When editing source files later, prefer versioned filenames and `old versions/` subfolders per user preference (does not apply to `ai_notes/`).

## Planned refactor (summary)

See [00_MASTER_PLAN_v2.txt](00_MASTER_PLAN_v2.txt). Phased order: instrument on device → heatmap/weights → Common Typo List + settings → **literal dictionary-narrowing swipe engine** (no swipe library) → optional key geometry toggles.

Likely touch points when coding starts:

- `Suggest.kt`, `InputLogic.java`, `DictionaryFacilitatorImpl.kt`
- `PointerTracker.java`, `BatchInputArbiter.java`, `ProximityInfo.java`
- `app/src/main/jni/src/suggest/**` (typing + gesture policies, `proximity_info.cpp`, `suggest.cpp`)

Do not assume AOSP behavior is sacred — the goal is replacement, not tweaking thresholds only.

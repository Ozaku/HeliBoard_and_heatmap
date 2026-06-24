# Dictionaries and learning

All suggestion and spell-check quality ultimately depends on **binary Patricia-trie dictionaries** loaded into `BinaryDictionary` native handles, plus optional personalized/user sources merged at query time.

## Dictionary types

Defined in `app/src/main/java/helium314/keyboard/latin/dictionary/Dictionary.java`:

| Constant | Value | Typical source |
|----------|-------|----------------|
| `TYPE_MAIN` | `main` | Downloaded/cached language `.dict` from assets metadata |
| `TYPE_USER` | `user` | Android user dictionary |
| `TYPE_USER_HISTORY` | `history` | Learned words (personalization) |
| `TYPE_CONTACTS` | `contacts` | Contact names |
| `TYPE_APPS` | `apps` | App name corpus (optional) |
| `TYPE_EMOJI` | `emoji` | Emoji suggestions (separate from word dict) |

## Loading and coordination

| File | Role |
|------|------|
| `DictionaryFacilitatorImpl.kt` | `resetDictionaries()`, parallel multilingual queries, merge results |
| `DictionaryFacilitatorLruCache.java` | Cache facilitators per locale |
| `DictionaryFactory.kt` | Builds `ReadOnlyBinaryDictionary`, collections |
| `SingleDictionaryFacilitator.kt` | Simpler single-locale facilitator |
| `DictionaryFacilitatorProvider` | App-wide accessor from `LatinIME` |
| `personalization/UserHistoryDictionary.java` | Writable history dict |
| `personalization/PersonalizationHelper.java` | Triggers learning on commit |
| `ContactsBinaryDictionary.java` | Contacts integration |
| `AppsBinaryDictionary.java` | Installed app names |
| `ReadOnlyBinaryDictionary` / `BinaryDictionary.java` | File-backed trie access |

`resetDictionaries()` loads subset based on settings: contacts, apps, `usePersonalizedDicts` → history dict (`DictionaryFacilitatorImpl.kt:128+`).

## Assets and on-disk files

In repo (metadata, not full word lists):

- `app/src/main/assets/dictionaries_in_dict_hashes.txt`
- `app/src/main/assets/known_dict_hashes.txt`

Runtime: dictionaries extracted/downloaded to app storage; **debug builds** may exclude large `main_ro.dict` via `app/build.gradle.kts` asset ignore pattern for APK size.

User can add custom main dictionaries via settings (`DictionaryScreen.kt`, `NewDictionaryDialog.kt`).

## Query flow

1. `Suggest` / spell checker asks `DictionaryFacilitatorImpl.getSuggestionResults()`
2. For each enabled dictionary of locale, call `getSuggestions` with `ComposedData` + `NgramContext`
3. Results merged into `SuggestionResults` (scores, source dict, flags)
4. Gesture mode: extra garbage filtering on main/history for batch input

## Learning (personalization)

On word commit (space, pick, etc.):

- `PersonalizationHelper` → `UserHistoryDictionary.addToDictionary()`
- Native: `BinaryDictionary.updateEntriesForWord` / `updateEntriesForWordNative`

History dict is **expandable binary format** on disk — influences future suggestion scores, not just a flat word list.

**Interaction with autocorrect:** `Suggest.kt` notes that personalization may warrant re-evaluating “no main dict → no autocorrect” rule.

## Spell checker

`AndroidSpellCheckerService` — separate `DictionaryFacilitatorImpl` instance; shares dict loading patterns but independent lifecycle from IME strip.

## Emoji dictionaries

- Tool: `tools/make-emoji-keys` builds emoji key data from UCD `emoji-test.txt`
- Runtime emoji suggestion via `TYPE_EMOJI` and emoji keyboard modules under `keyboard/emoji/`

## Multilingual

`DictionaryFacilitatorImpl` can query multiple sub-dictionaries per locale group; weights in `DictionaryCollection` / facilitator config.

## Refactor notes

| Goal | Touch |
|------|-------|
| Stop “learning” wrong swipe words | History update on commit path; batch vs tap distinction |
| Simpler dict stack | `DictionaryFacilitatorImpl`, which types to load |
| Custom word list only | Replace main dict pipeline; keep or drop contacts/history merge |
| New binary format | JNI `dictionary/` tree, `DictionaryFactory` |

Related: [03_autocorrect_and_suggestions.md](03_autocorrect_and_suggestions.md), [06_native_and_build.md](06_native_and_build.md)

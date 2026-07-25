# ThoughtCanvas — Implementation Plan V2

Follow-along checklist. Tick each `[ ]` as we go. Every step ends with a **Verify** gate — do not proceed past a red gate.

## Decisions Locked (2025)

| Topic | Decision |
|---|---|
| Architecture | Android-only for MVP; refactor to KMP later. Hilt + Room stay. |
| Module strategy | Start with `:app` + `:core:database` + `:core:storage`; split on demand. |
| Devices | Tablet-only. Primary: Xiaomi Pad 8 Pro (Focus Pen Pro). Secondary: Boox Note 4C (generic stylus, e-ink). |
| SDK | `minSdk = 28`, `targetSdk = 35`, `compileSdk = 35`, Java/Kotlin JVM target 21. |
| Package | `io.github.mafflerbach.thoughtcanvas` |
| Repo | Public GitHub under `mafflerbach/ThoughtCanvas`. Conventional Commits. |
| Drawing engine | Jetpack **`androidx.ink`** (Ink API). |
| Storage root | User-picked SAF folder (default suggestion: `Documents/ThoughtCanvas/`), URI in DataStore. |
| Canvas format | `canvas.json` — schema defined in Phase 3 below. |
| AI (later) | ML Kit Digital Ink (OCR, offline) + Gemini (BYO-key in DataStore) as first real providers. |
| Testing | JVM unit tests + Android instrumentation tests. No Robolectric. |
| CI | GitHub Actions on public repo. |
| MVP non-goals | No cloud sync (delegated to Syncthing etc.), no encryption, no i18n, no export/import, no OCR/AI. |

---

## Phase 0 — Project bootstrap

Goal: empty tablet-optimized app boots on the Xiaomi Pad, CI is green.

- [ ] `git init`, add `.gitignore` (Android + IntelliJ + Gradle)
- [ ] Create GitHub repo `mafflerbach/ThoughtCanvas` (public), push `main`
- [ ] Create Gradle project with version catalog `gradle/libs.versions.toml`
- [ ] Root `build.gradle.kts` + `settings.gradle.kts` with modules: `:app`, `:core:database`, `:core:storage`
- [ ] `:app` module: applicationId `io.github.mafflerbach.thoughtcanvas`, Compose enabled, Material 3, Hilt, Navigation Compose
- [ ] `:core:database`: Room + KSP
- [ ] `:core:storage`: DataStore + SAF helpers (no impl yet, just module skeleton)
- [ ] Add ktlint, Detekt, Spotless as convention plugins in `build-logic/`
- [ ] `MainActivity` with empty Compose scaffold showing "ThoughtCanvas" + M3 theme
- [ ] AndroidManifest: `android:resizeableActivity="true"`, landscape+portrait, no orientation lock, request stylus features
- [ ] GitHub Actions workflow `.github/workflows/ci.yml`: JDK 21, `./gradlew ktlintCheck detekt test assembleDebug`
- [ ] ADR-0002: "Android-only MVP with KMP-ready boundaries"
- [ ] ADR-0003: "SAF folder as filesystem root"

**Verify:**
```bash
./gradlew ktlintCheck detekt test assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
# App launches on Xiaomi Pad, shows scaffold, no crash.
```
CI badge is green on `main`.

---

## Phase 1 — Daily Journal MVP

Goal: open today's entry, type markdown, draw ink, insert photo, tag it, persist to SAF folder and Room index.

### 1.1 — Storage foundation (`:core:storage`)
- [x] `StorageRoot` sealed type
- [x] `SafFileRepository` using `DocumentFile`
- [x] First-run flow: `ACTION_OPEN_DOCUMENT_TREE`, `takePersistableUriPermission`, persist URI in DataStore (`StorageRootPreferences`)
- [x] `JournalPathResolver`: given `LocalDate` → `Journal/YYYY/MM/DD/`
- [x] `FileRepository` interface with `readText`, `writeText`, `writeBytes`, `list`, `delete`
- [x] `StorageRootState` (app-scoped `StateFlow`) + Hilt wiring
- [x] Unit tests: `JournalPathResolverTest`, `InMemoryFileRepositoryTest`
- [ ] Instrumentation test against real SAF (deferred to next slice — needs emulator/device)

**Verify:** instrumentation test writes `journal.md`, reads it back, path visible via Files app.

### 1.2 — Database index (`:core:database`)
- [x] Entities: `JournalEntryEntity(date PK, updatedAt, createdAt)`, `TagEntity(id, name unique)`, `EntryTagCrossRef` (cascade FKs), `AttachmentEntity(id, entryDate, kind, relativePath, createdAt)`
- [x] DAOs (`JournalEntryDao`, `TagDao`, `AttachmentDao`) with `Flow` reads and suspend writes
- [x] `ThoughtCanvasDatabase` (Room 2.8.4, KSP 2.3.6, schema export at `core/database/schemas/`)
- [x] Hilt module `DatabaseModule`
- [x] `MigrationTest` skeleton using `MigrationTestHelper`
- [x] `JournalIndexRepository` (transactional upsert, tag replace, cascade-delete)

**Verify:** `./gradlew :core:database:connectedDebugAndroidTest` → 7/7 tests passed on Xiaomi Pad.

### 1.3 — Feature module `:feature:journal`
- [ ] Add module to `settings.gradle.kts`
- [ ] MVVM: `JournalViewModel`, `JournalUiState`, `JournalRepository` (composes `FileRepository` + `JournalIndexRepository`)
- [ ] Screen: `TodayJournalScreen` — top bar with date, tag chips, three-pane layout (markdown | canvas | attachments)
- [ ] Markdown editor: pick **Markwon** for rendering + plain `TextField` for editing (split view). Alt: `compose-richtext`. Decide during 1.3.
- [ ] Tag input: chip row + freeform `TextField`, no autocomplete yet
- [ ] Photo picker: `PickVisualMedia` contract → copy bytes into `images/` under today's folder

**Verify:** manual — create today's entry on device, add markdown text + tag + photo, kill app, reopen, everything is still there.

### 1.4 — Ink canvas (`:feature:canvas`)
- [ ] Add `androidx.ink:ink-*` dependencies (authoring, rendering, strokes, geometry, brush)
- [ ] `InkCanvasScreen` embedded inside `TodayJournalScreen`
- [ ] `InProgressStrokesView` + `CanvasStrokeRenderer` per Ink API sample
- [ ] Serialize finished `Stroke`s to `canvas.json` (schema in Phase 3, use v1 minimal now)
- [ ] Pan + zoom via `Modifier.transformable`; infinite canvas backed by transform matrix
- [ ] Pen input filtering: only `MotionEvent.TOOL_TYPE_STYLUS` draws; finger pans
- [ ] Undo/redo stack in ViewModel (list of stroke ops)

**Verify:** stylus draws with pressure on Xiaomi Pad; finger pans; reopen entry → strokes still there.

### 1.5 — Phase 1 acceptance
- [ ] All ADRs updated
- [ ] README updated with screenshots
- [ ] `./gradlew check connectedDebugAndroidTest` green
- [ ] Manual test script in `docs/QA_PHASE1.md` executed on both tablets

---

## Phase 2 — Drawing engine polish

Only after Phase 1 ships.

- [ ] Tilt-aware brushes via Ink API brush families
- [ ] Lasso selection: hit-test strokes inside a lasso path (Ink geometry API)
- [ ] Move/scale/delete on selection
- [ ] Persistent undo history across sessions
- [ ] Boox e-ink perf pass: disable predicted strokes when refresh rate < 30Hz
- [ ] Benchmarks: `androidx.benchmark` for stroke serialization

---

## Phase 3 — Storage v1 (formalize)

Directory layout (final):

```
<SAF-root>/
  Journal/
    YYYY/
      MM/
        DD/
          metadata.json     # date, tags[], createdAt, updatedAt, schemaVersion
          journal.md        # user markdown
          canvas.json       # ink strokes (schema below)
          images/*.jpg      # copied photos
          attachments/*     # future
  .thoughtcanvas/
    index.db-backup         # optional Room export
```

### `canvas.json` schema v1

```jsonc
{
  "schemaVersion": 1,
  "canvasSize": { "width": 4096, "height": 4096, "unit": "px" },
  "viewport": { "x": 0, "y": 0, "zoom": 1.0 },
  "strokes": [
    {
      "id": "uuid-v4",
      "tool": "pen",                 // pen | pencil | highlighter | eraser
      "brush": {
        "family": "pressure-pen-v1", // Ink brush family identifier
        "color": "#RRGGBBAA",
        "size": 2.4,
        "epsilon": 0.1
      },
      "inputs": [
        {
          "x": 123.4, "y": 567.8,
          "t": 12,                   // ms since stroke start
          "pressure": 0.72,
          "tiltX": 0.10, "tiltY": -0.05,
          "orientation": 1.57
        }
      ],
      "createdAt": "2025-01-01T12:34:56Z"
    }
  ]
}
```

Rationale: mirrors `androidx.ink.strokes.StrokeInputBatch` field-for-field so (de)serialization is lossless; forward-compatible via `schemaVersion`.

- [ ] `CanvasSerializer` with kotlinx.serialization
- [ ] Round-trip property tests (draw → save → load → identical strokes)
- [ ] ADR-0004: canvas.json schema

---

## Phase 4 — AI abstraction

- [ ] `:core:ai` module
- [ ] `interface AiProvider { recognizeHandwriting; summarize; generateTags; embeddings }`
- [ ] `MlKitDigitalInkProvider` (offline OCR) — first real impl
- [ ] `GeminiProvider` (BYO API key stored encrypted in DataStore via `EncryptedSharedPreferences` bridge)
- [ ] Provider selection UI in `:feature:settings`
- [ ] Explicit region selection UX: user lassos ink → "Convert to text"

---

## Phase 5 — Knowledge graph

Out of scope for this plan revision. Revisit after Phase 4.

---

## Cross-cutting rules (always on)

- Every PR: green CI, updated tests, ADR if architecture changes
- Every module has an `AGENTS.md` when non-obvious
- No business logic in `@Composable`s
- Repository interfaces in `:core:*`, impls injected by Hilt
- Conventional Commits: `feat(canvas): add pressure serialization`

---

## Next action

Ready to execute Phase 0 step-by-step. Say the word and I'll start with `.gitignore` + Gradle scaffolding.

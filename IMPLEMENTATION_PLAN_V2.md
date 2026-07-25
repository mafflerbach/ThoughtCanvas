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

### 1.4 — Ink canvas (`:feature:canvas`) — **frozen after Slice A**
- [x] Add `androidx.ink:ink-*` dependencies (authoring, rendering, strokes, geometry, brush, storage, authoring-compose) at 1.0.0
- [x] `InkCanvas` composable using `androidx.ink.authoring.compose.InProgressStrokes` + `CanvasStrokeRenderer`
- [x] `pressurePen` stock brush, verified on Focus Pen Pro
- [x] Embedded inside `TodayJournalScreen` as a fixed-height pane (temporary; superseded by Phase 2 block canvas)
- [ ] ~~Persist finished strokes to disk~~ — **dropped**, superseded by Phase 2 canvas manifest
- [ ] ~~Pan/zoom, stylus-only filter, undo/redo~~ — **dropped**, superseded by Phase 2

Rationale: Phase 1.3 verified end-to-end file/DB plumbing on device. Continuing
to build persistence + undo on the flat UI would produce throwaway code because
ADR-0004 pivots the app to a block-based canvas. See ADR-0004.

### 1.5 — Phase 1 acceptance — **abbreviated**
- [x] Phase-1 flat MVP runs on Xiaomi Pad 8 Pro end-to-end (folder picker, journal, tags, photos, live ink drawing)
- [ ] ~~README screenshots, QA script on both tablets~~ — **dropped**, we no longer polish the flat UI

---

## Phase 2 — Block canvas (major pivot)

Defined by **ADR-0004**. Supersedes the flat journal layout with a
block-based canvas as the primary editing surface.

### 2.1 — Data model + storage redesign
- [ ] New Room schema v2: `canvases`, `blocks`, `edges`, `canvas_tags`, `block_tags`. Data-loss migration acceptable (see ADR-0004).
- [ ] `CanvasManifest` kotlinx.serialization model matching ADR-0004 schema
- [ ] `CanvasRepository` in `:feature:canvas`: load/save `*.canvas.json`, resolve `ref` paths relative to canvas location
- [ ] `InkFile` model + `InkFileRepository` for `<regionId>.ink.json` and `<canvas>.floating.ink.json`
- [ ] Migration to new layout (`journal.md` + `daily.canvas.json` per day)
- [ ] Unit tests: manifest round-trip, ink file round-trip, ref resolution

### 2.2 — Canvas rendering shell (`:feature:canvas`)
- [ ] Bounded (8192×8192) world with `Modifier.transformable` pan/zoom
- [ ] `CanvasScreen` replaces `TodayJournalScreen` as the entry point post-onboarding
- [ ] Block layer: place-holder frames with drag/resize/delete
- [ ] Ink layer: floating strokes on top; per-region strokes rendered inside their block bounds
- [ ] Stylus-only ink capture; finger pans; two-finger pinch zooms
- [ ] Autosave on move/edit (debounced) writing back to the manifest

### 2.3 — Block kinds
- [ ] `markdown-embed` — in-place editable Compose editor writing directly to the referenced `.md` file, with YAML frontmatter tag parsing
- [ ] `image-embed` — read-only rendering via existing `SafPathResolver`
- [ ] `ink-region` — nested ink authoring within the block bounds; strokes stored in region-local coordinates in a separate `.ink.json` file

### 2.4 — Edges
- [ ] Directed edges between blocks with optional labels
- [ ] Auto-routing from block centers; explicit sides in schema when set
- [ ] Edge draw layer above blocks, below floating ink

### 2.5 — Daily-canvas UX
- [ ] On launch: open today's `Journal/YYYY/MM/DD/daily.canvas.json`
- [ ] If missing, bootstrap: create the folder, empty `journal.md`, and a canvas containing a single `markdown-embed` block pointing at it
- [ ] Simple date-picker in the top bar to hop to another day

### 2.6 — Undo, selection, retire flat UI
- [ ] Undo/redo covering block create/move/resize/delete and ink strokes
- [ ] Lasso selection: box-select blocks and floating strokes; group move
- [ ] Delete `:feature:journal` flat screen once daily canvas is stable

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

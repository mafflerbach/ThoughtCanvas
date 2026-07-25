# ADR-0004 — Block-based canvas as the primary editing surface

## Status
Proposed

## Context

Phase 1 shipped a *flat* daily-journal UI: a stacked layout of markdown
editor, image strip, and a single ink surface. This proved the plumbing
(SAF storage, Room index, ink authoring), but the layout was always meant
as a shortcut.

`docs/PRODUCT_VISION.md` describes ThoughtCanvas as a "document platform
composed of blocks". The flat UI does not deliver that. Continuing to
polish it would build UI we will throw away.

Obsidian's Canvas view is the closest existing model. We take mental
inspiration from it (spatial 2D surface, blocks, edges) but not the
`.canvas` file schema — we need our own schema tuned for ink and for
embed-first semantics.

## Decision

We pivot Phase 2 to a **block-based canvas** as the primary editing
surface, replacing the flat journal layout.

### Guiding invariants

1. **Files are truth.** Every block in a canvas references a file on
   disk. There is no inline block content.
2. **Markdown stays portable.** Every markdown block is a real `.md`
   file readable by any Markdown viewer, including Obsidian.
3. **Canvases compose; they don't own content.** A canvas manifest is
   layout metadata plus references. Deleting the manifest never deletes
   user content.
4. **Ink is a first-class file type**, not a database blob.

### Canvas file layout (v1 schema)

Canvas manifest: `*.canvas.json`.

```jsonc
{
  "schemaVersion": 1,
  "id": "uuid-v4",
  "createdAt": 1735689600000,
  "updatedAt": 1735689600000,
  "tags": ["daily", "work"],
  "world": { "width": 8192, "height": 8192 },
  "viewport": { "x": 0, "y": 0, "zoom": 1.0 },

  "blocks": [
    {
      "id": "uuid",
      "kind": "markdown-embed",
      "x": 120, "y": 80,
      "width": 480, "height": 320,
      "z": 0,
      "ref": "journal.md"
    },
    {
      "id": "uuid",
      "kind": "image-embed",
      "x": 640, "y": 80,
      "width": 320, "height": 240,
      "z": 1,
      "ref": "images/2025-07-25-sunset.jpg"
    },
    {
      "id": "uuid",
      "kind": "ink-region",
      "x": 60,  "y": 460,
      "width": 900, "height": 420,
      "z": 2,
      "ref": "8f4e2c11.ink.json"
    }
  ],

  "floatingStrokes": {
    "ref": "daily.floating.ink.json"
  },

  "edges": [
    {
      "id": "uuid",
      "from": { "blockId": "…", "side": "right" },
      "to":   { "blockId": "…", "side": "left"  },
      "label": "explains"
    }
  ]
}
```

**Design notes**

- All `ref` values are **relative to the canvas file's own directory**,
  the way Markdown image links behave. This keeps a canvas self-contained
  if its whole folder is copied elsewhere.
- Block `x, y, width, height` are in world coordinates (device-independent
  units, treated as logical pixels for now).
- `z` is a stable stacking key managed by the app; blocks in the array
  are still stored in the order they were created, so `z` alone
  determines paint order.
- `edges[].from/to.side` ∈ `{ "top", "right", "bottom", "left" }`.
  Optional; if omitted the app auto-routes from block center.

### Ink files

Per-region ink file (`<regionId>.ink.json`):

```jsonc
{
  "schemaVersion": 1,
  "id": "uuid-matching-block-id-or-independent",
  "tags": ["sketch", "idea"],
  "createdAt": …, "updatedAt": …,
  "strokes": [
    {
      "id": "uuid",
      "brush": { "family": "pressure-pen-v1", "color": "#000000FF", "size": 4.0, "epsilon": 0.1 },
      "inputs": [
        { "x": 12.3, "y": 45.6, "t": 0,  "pressure": 0.42, "tiltX": 0.1, "tiltY": 0.0, "orientation": 1.57 }
      ],
      "createdAt": …
    }
  ]
}
```

- **Strokes inside an ink-region are stored in region-local coordinates**
  (0,0 = top-left of the region). Moving the region does not rewrite the
  stroke file.
- **Floating strokes** in `<canvas>.floating.ink.json` are stored in
  world coordinates. Same schema otherwise.

### Block content contract

| Kind             | `ref` points at            | Editing surface                          |
|------------------|----------------------------|------------------------------------------|
| `markdown-embed` | `*.md` file                | In-block Compose editor writing to disk. |
| `image-embed`    | An image file              | Read-only in Phase 2.                    |
| `ink-region`     | An `.ink.json` file        | Ink authoring inside the region bounds.  |

### Database (Room) index — v2 schema, breaking

Existing schema is wiped. Approved: the only production data so far is
test scribbles.

New tables:

- `canvases` — `id (PK, uuid)`, `path`, `title`, `updatedAt`, `createdAt`
- `blocks` — `id (PK, uuid)`, `canvasId (FK)`, `kind`, `x`, `y`, `width`,
  `height`, `z`, `ref (nullable for future inline)`, `updatedAt`
- `edges` — `id (PK, uuid)`, `canvasId (FK)`, `fromBlockId`, `toBlockId`,
  `fromSide (nullable)`, `toSide (nullable)`, `label (nullable)`
- `canvas_tags` — `canvasId + tagId` cross ref (tags themselves reused
  from v1)
- `block_tags` — `blockId + tagId` cross ref (populated by scanning
  referenced files' frontmatter / ink-region metadata)
- `journal_entries` — retained; still one-per-day, but its role becomes
  "does today's daily canvas exist?" Not required for MVP, may be
  removed later.

Foreign keys use `@Upsert` for parent updates (learned this the hard way
in ADR-0004's predecessor PR #4).

### Application launch

On launch the app opens today's `Journal/YYYY/MM/DD/daily.canvas.json`.
If missing, it is created together with an empty `journal.md` and a
canvas containing one `markdown-embed` block pointing at that file. This
preserves the daily-journal metaphor without special-casing daily
canvases in the schema.

### Directory layout

Reaffirms the layout in `PRODUCT_VISION.md`:

```
<root>/
  Journal/YYYY/MM/DD/
    journal.md
    daily.canvas.json
    <regionId>.ink.json
    daily.floating.ink.json
    images/
  Canvases/
    Ideas.canvas.json
    Ideas/                 (optional sibling asset dir)
  Documents/
```

### File naming

Canvas files use the `.canvas.json` double extension. Rationale:

- Honest about being JSON so any editor can read them.
- The `.canvas` prefix flags them to file-system watchers and future
  export tools.
- We are explicitly *not* using Obsidian's `.canvas` (see A.1 in the
  Product Vision grilling).

## Consequences

**Positive**

- Matches the product vision from day one of Phase 2.
- Nothing built in Phase 2 will be thrown away when we add lazy loading,
  more block types, or infinite pan/zoom later.
- Files remain portable. Sync stays a folder-watching concern.

**Negative / accepted**

- Phase 1's flat UI is retired. The `TodayJournalScreen` composable is
  gone in Phase 2, but its supporting infrastructure (`FileRepository`,
  SAF, Ink authoring plumbing) stays.
- The Room schema is a breaking change with no migration. Acceptable
  because there is no real user data yet.
- Pan/zoom + block dragging is a real chunk of Compose work. That is
  Phase 2's cost of admission.

## Related

- ADR-0001 — Filesystem as source of truth. Reaffirmed.
- ADR-0002 — Android-only MVP. Reaffirmed.
- ADR-0003 — SAF root. Reaffirmed.
- `docs/PRODUCT_VISION.md` — expanded companion doc.

## Open questions for follow-up ADRs

- ADR-0005 (future): infinite canvas & coordinate precision when we lift
  the 8192×8192 bound.
- ADR-0006 (future): AI provider contract for ink-to-markdown that
  produces new blocks with proper references.
- ADR-0007 (future): conflict handling when Syncthing brings back two
  canvas manifests with divergent block moves.

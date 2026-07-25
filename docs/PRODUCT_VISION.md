# Product Vision

ThoughtCanvas is a personal knowledge system where **everything happens on a
canvas** and every idea is a **standalone file**.

It is not a note-taking app. It is not an Obsidian clone. It does not want
you to type structured markdown into a text editor and hope a graph view
saves your thinking later.

## Core principles

1. **Ink first.** Handwriting is a primary input, not an afterthought.
2. **Structure later.** Users write, draw, embed, and rearrange freely.
   Structure emerges by *placing things next to each other* on the canvas.
3. **Embed first.** Nothing lives inline inside a canvas. Every block is a
   reference to a file on disk. Files are edited in-place; canvases are
   composition views.
4. **AI understands afterwards.** AI never overwrites user ink or text. It
   annotates, suggests, and translates on request.
5. **Offline first.** Everything must work with airplane mode on.
6. **User owns the data.** The filesystem is the source of truth. The
   database is an index that can be rebuilt from scratch.

## The block canvas

A canvas is a 2D surface (large but finite in Phase 2 — infinite later)
holding **blocks** and **freely-floating ink strokes**. Blocks are
rectangles at world coordinates. They *embed* content from files.

```
┌───────────── daily.canvas.json ─────────────┐
│                                              │
│   ┌──────────────┐        ✒️  floating       │
│   │ ![[journal.md]]│           ink here     │
│   └──────────────┘                          │
│                                              │
│   ┌───────────┐    ─────►   ┌───────────┐  │
│   │ image.jpg │              │  ink      │  │
│   └───────────┘              │  region   │  │
│                              └───────────┘  │
└──────────────────────────────────────────────┘
```

### Block types (Phase 2 MVP)

| Kind             | What it embeds                                     |
|------------------|----------------------------------------------------|
| `markdown-embed` | A `.md` file. Editable in place; the file is still readable in any Markdown viewer. |
| `image-embed`    | An image file (jpg/png/webp/…).                    |
| `ink-region`     | A rectangle grouping ink strokes stored in a separate `<id>.ink.json` file. Move the region → strokes move with it. |

**There is no `markdown-inline` block type.** All markdown lives in real
`.md` files.

Deferred to later phases: `pdf-embed`, `audio-embed`, `tasks`, `ai-annotation`,
`link-to-day`.

### Ink

Ink strokes live in two places:

- **Grouped into `ink-region` blocks**, whose strokes live in a per-region
  `.ink.json` file for lazy loading and portability.
- **Freely floating on the canvas** at world coordinates, stored in a
  single canvas-scoped ink file (e.g. `<canvasId>.floating.ink.json`).

Ink is never drawn *on top of* a markdown block. Overlap is handled by
block z-order. This is a deliberate simplification: no compositing rules,
no "which stroke belongs to which block" ambiguity.

### Edges

Blocks can be connected by directed edges (Obsidian-canvas style). Edges
have optional labels. They are stored in the canvas manifest, not in
individual block files.

### Tags

Two levels:

- **Canvas-level tags** live in the canvas manifest and describe the
  canvas as a whole.
- **Block-level tags** live *inside the referenced file*:
  - Markdown blocks use YAML frontmatter (standard convention).
  - Ink-region files carry a top-level `tags` array.
  - Images have no in-file tag mechanism yet; deferred.

The database indexes both levels so search sees everything.

## Directory layout

The SAF-picked root looks like this in Phase 2:

```
<root>/
  Journal/
    YYYY/MM/DD/
      journal.md                 ← standalone markdown file
      daily.canvas.json          ← the canvas manifest for the day
      <regionId>.ink.json        ← per-ink-region stroke files
      <canvasId>.floating.ink.json  ← floating strokes for this canvas
      images/
        <uuid>.jpg               ← attachments still live here
      attachments/
  Canvases/                       ← user-created ad-hoc canvases
    Ideas.canvas.json
    Ideas/                       ← optional sibling folder for its assets
      <regionId>.ink.json
      images/
  Documents/                      ← arbitrary markdown, PDFs, images
    2025-Q1-plan.md
```

Everything the user puts under `<root>/` is fair game. ThoughtCanvas is
opinionated about `Journal/YYYY/MM/DD/` because that's the daily journal
metaphor, but users can create canvases anywhere.

## What the app opens on launch

The **daily canvas for today** (`Journal/YYYY/MM/DD/daily.canvas.json`).
If it doesn't exist yet, ThoughtCanvas creates one containing a single
`markdown-embed` block pointing at today's `journal.md` (also freshly
created, empty).

A vault-wide file browser / recent-canvases list is a later phase.

## AI actions (still)

- Convert selected ink to Markdown (as a new `markdown-embed` block).
- Summarize selected blocks.
- Translate.
- Rewrite.
- Generate tags for selected blocks.
- Link related canvases.
- Export a canvas as static Markdown or an Obsidian-compatible view.

## Sync

The user's chosen sync tool (Syncthing, Nextcloud, rclone, git via
Termux — anything that watches a folder) operates on the SAF root.
ThoughtCanvas never manages sync itself. Because everything is
files-on-disk, conflict resolution happens at the file level and rarely
touches more than one thing at a time.

## Explicit non-goals

- Not an Obsidian clone. We don't need `.canvas` schema compatibility.
- Not a whiteboarding tool. Freeform ink is a first-class citizen, but
  the target is knowledge work, not brainstorming sessions.
- Not a wiki. Bidirectional link graphs are not the primary UI.
- Not cloud-native. There is no ThoughtCanvas server, and there will not
  be one in the foreseeable future.

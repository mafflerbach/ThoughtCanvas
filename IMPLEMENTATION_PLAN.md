# Initial Implementation Plan

## Phase 0
- Setup Android Studio
- Java 21
- Android SDK
- Kotlin
- Jetpack Compose
- Material 3
- Room
- Hilt
- Coroutines
- KSP
- Coil
- DataStore
- Navigation Compose
- Markdown renderer
- ktlint
- Detekt
- Spotless
- GitHub Actions

Verify:

./gradlew test
./gradlew assembleDebug

## Development

Repository layout:

app/
core/
    ai/
    database/
    drawing/
    markdown/
    search/
    storage/
feature/
    canvas/
    journal/
    timeline/
    search/
    settings/
docs/
architecture/
adr/

## Phase 1
Daily journal
- Infinite canvas
- Markdown
- Photos
- Tags

## Phase 2
Drawing engine
- Vector strokes
- Pressure
- Tilt
- Undo
- Redo
- Pan
- Zoom
- Lasso selection

## Phase 3
Storage

Journal/
    YYYY/
        MM/
            DD/
                metadata.json
                journal.md
                canvas.json
                images/
                attachments/

SQLite indexes files but is not the source of truth.

## Phase 4
AI abstraction

interface AiProvider
- recognizeHandwriting
- summarize
- generateTags
- embeddings

Providers:
- Google ML Kit
- Gemini
- OpenAI
- Ollama

## Phase 5
Knowledge graph
Documents
 -> Regions
 -> Ink
 -> Recognized text
 -> Embeddings
 -> Relationships

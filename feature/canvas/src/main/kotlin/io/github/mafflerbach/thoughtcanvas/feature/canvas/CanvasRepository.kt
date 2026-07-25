package io.github.mafflerbach.thoughtcanvas.feature.canvas

import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.CanvasManifest
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and saves `*.canvas.json` manifests through the storage-root
 * [FileRepository]. Does not touch ink files or the database index — those
 * are separate concerns.
 *
 * Paths given here are always storage-root-relative
 * (e.g. `Journal/2025/01/17/daily.canvas.json`).
 */
@Singleton
class CanvasRepository
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        private val json: Json = DefaultJson

        /** Returns `null` if the file does not exist or fails to parse. */
        suspend fun load(canvasPath: String): CanvasManifest? {
            val raw = fileRepository.readText(canvasPath) ?: return null
            return runCatching { json.decodeFromString(CanvasManifest.serializer(), raw) }.getOrNull()
        }

        suspend fun save(canvasPath: String, manifest: CanvasManifest) {
            val encoded = json.encodeToString(CanvasManifest.serializer(), manifest)
            fileRepository.writeText(canvasPath, encoded)
        }

        /** Convenience: resolve a block/edge/ink `ref` string to a root-relative path. */
        fun resolveRef(canvasPath: String, ref: String): String =
            CanvasPaths.resolveRef(canvasPath, ref)

        private companion object {
            val DefaultJson = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
    }

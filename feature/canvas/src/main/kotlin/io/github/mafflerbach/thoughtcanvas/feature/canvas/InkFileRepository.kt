package io.github.mafflerbach.thoughtcanvas.feature.canvas

import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.InkFile
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and saves `*.ink.json` files (per-region and floating). Storage
 * paths are root-relative and typically resolved through
 * [CanvasRepository.resolveRef] before being handed here.
 */
@Singleton
class InkFileRepository
    @Inject
    constructor(
        private val fileRepository: FileRepository,
    ) {
        private val json: Json = DefaultJson

        suspend fun load(path: String): InkFile? {
            val raw = fileRepository.readText(path) ?: return null
            return runCatching { json.decodeFromString(InkFile.serializer(), raw) }.getOrNull()
        }

        suspend fun save(path: String, file: InkFile) {
            val encoded = json.encodeToString(InkFile.serializer(), file)
            fileRepository.writeText(path, encoded)
        }

        private companion object {
            val DefaultJson = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
    }

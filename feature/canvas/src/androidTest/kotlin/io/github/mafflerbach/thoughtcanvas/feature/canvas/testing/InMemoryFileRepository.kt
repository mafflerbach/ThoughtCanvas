package io.github.mafflerbach.thoughtcanvas.feature.canvas.testing

import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository

/**
 * Copy of the shared in-memory fake. Kept here rather than depending on
 * `:feature:journal` because that would create a canvas → journal edge
 * across modules that will not exist for long.
 */
class InMemoryFileRepository : FileRepository {
    private val files = mutableMapOf<String, ByteArray>()
    private val directories = mutableSetOf("")

    override suspend fun exists(path: String): Boolean {
        val key = path.normalise()
        return files.containsKey(key) || directories.contains(key)
    }

    override suspend fun createDirectories(path: String) {
        val segments = path.normalise().split('/').filter { it.isNotEmpty() }
        var current = ""
        for (segment in segments) {
            current = if (current.isEmpty()) segment else "$current/$segment"
            directories.add(current)
        }
    }

    override suspend fun readText(path: String): String? =
        files[path.normalise()]?.decodeToString()

    override suspend fun writeText(path: String, content: String) {
        writeBytes(path, content.encodeToByteArray())
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        val key = path.normalise()
        require(key.isNotEmpty()) { "path must not be empty" }
        val parent = key.substringBeforeLast('/', missingDelimiterValue = "")
        if (parent.isNotEmpty()) createDirectories(parent)
        files[key] = bytes
    }

    override suspend fun list(path: String): List<String> {
        val key = path.normalise()
        if (key !in directories) return emptyList()
        val prefix = if (key.isEmpty()) "" else "$key/"
        val children = mutableSetOf<String>()
        (files.keys + directories).forEach { candidate ->
            if (candidate.startsWith(prefix) && candidate != key) {
                val head = candidate.removePrefix(prefix).substringBefore('/')
                if (head.isNotEmpty()) children.add(head)
            }
        }
        return children.sorted()
    }

    override suspend fun delete(path: String): Boolean {
        val key = path.normalise()
        val hadFile = files.remove(key) != null
        val hadDir = directories.remove(key)
        if (hadDir) {
            val prefix = "$key/"
            files.keys.filter { it.startsWith(prefix) }.forEach { files.remove(it) }
            directories.removeAll { it.startsWith(prefix) }
        }
        return hadFile || hadDir
    }

    fun files(): Map<String, ByteArray> = files.toMap()

    private fun String.normalise(): String =
        trim('/').split('/').filter { it.isNotEmpty() }.joinToString("/")
}

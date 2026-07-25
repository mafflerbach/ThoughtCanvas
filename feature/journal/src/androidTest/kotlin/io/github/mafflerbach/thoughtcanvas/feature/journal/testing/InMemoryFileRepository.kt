package io.github.mafflerbach.thoughtcanvas.feature.journal.testing

import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository

/**
 * Local copy of the [FileRepository] test double. A duplicate of
 * `:core:storage`'s test-source variant, kept here so we don't publish it
 * from a testFixtures artifact just for one downstream test class.
 *
 * Not thread-safe. Paths are normalised (leading/trailing slashes stripped,
 * empty segments removed).
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

    fun snapshotFilePaths(): Set<String> = files.keys.toSet()

    private fun String.normalise(): String =
        trim('/').split('/').filter { it.isNotEmpty() }.joinToString("/")
}

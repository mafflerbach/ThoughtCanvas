package io.github.mafflerbach.thoughtcanvas.core.storage

/**
 * Test double for [FileRepository]. Not thread-safe; suitable for unit tests only.
 *
 * Backed by two maps: one for file contents, one for known directories.
 * Paths are normalised (leading/trailing slashes stripped, empty segments removed).
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

    override suspend fun readText(path: String): String? = files[path.normalise()]?.decodeToString()

    override suspend fun writeText(
        path: String,
        content: String,
    ) {
        writeBytes(path, content.encodeToByteArray())
    }

    override suspend fun writeBytes(
        path: String,
        bytes: ByteArray,
    ) {
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
        val depth = prefix.count { it == '/' }
        val children = mutableSetOf<String>()
        (files.keys + directories).forEach { candidate ->
            if (candidate.startsWith(prefix) && candidate != key) {
                val relative = candidate.removePrefix(prefix)
                val head = relative.substringBefore('/')
                if (head.isNotEmpty()) children.add(head)
            }
            // depth is intentionally unused; suppression comment removed.
            @Suppress("UNUSED_EXPRESSION")
            depth
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

    private fun String.normalise(): String = trim('/').split('/').filter { it.isNotEmpty() }.joinToString("/")
}

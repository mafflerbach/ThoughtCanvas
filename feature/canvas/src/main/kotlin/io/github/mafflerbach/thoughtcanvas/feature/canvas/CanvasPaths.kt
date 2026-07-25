package io.github.mafflerbach.thoughtcanvas.feature.canvas

/**
 * Path arithmetic in the storage-root-relative namespace used by
 * `FileRepository`. All inputs and outputs are `/`-separated, and paths are
 * treated as pure strings (no filesystem I/O happens here).
 *
 * Kept isolated so it can be unit-tested on the JVM with no Android deps.
 */
internal object CanvasPaths {
    /**
     * Resolve [ref] relative to the directory containing [canvasPath].
     *
     * - `ref = "journal.md"` under `Journal/2025/01/17/daily.canvas.json`
     *   resolves to `Journal/2025/01/17/journal.md`.
     * - `ref` may contain `..` segments; they are collapsed against the
     *   canvas directory.
     * - An absolute-looking `ref` starting with `/` is normalised to root-
     *   relative (leading slash stripped) rather than escaping the root.
     */
    fun resolveRef(canvasPath: String, ref: String): String {
        val canvasDirSegments = canvasPath.segments().dropLast(1)
        val refSegments = ref.trimStart('/').segments()
        val combined = (canvasDirSegments + refSegments).toMutableList()
        val normalised = mutableListOf<String>()
        for (segment in combined) {
            when (segment) {
                "." -> {}
                ".." -> if (normalised.isNotEmpty()) normalised.removeAt(normalised.lastIndex)
                else -> normalised.add(segment)
            }
        }
        return normalised.joinToString("/")
    }

    /** Directory portion of a file path, `""` if the file is at the root. */
    fun directoryOf(filePath: String): String = filePath.segments().dropLast(1).joinToString("/")

    private fun String.segments(): List<String> =
        split('/').filter { it.isNotEmpty() }
}

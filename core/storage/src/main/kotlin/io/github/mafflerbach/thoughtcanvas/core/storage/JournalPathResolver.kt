package io.github.mafflerbach.thoughtcanvas.core.storage

import java.time.LocalDate
import javax.inject.Inject

/**
 * Resolves the on-disk layout for a journal entry:
 *
 * ```
 * <root>/Journal/YYYY/MM/DD/
 *     metadata.json
 *     journal.md
 *     canvas.json
 *     images/
 *     attachments/
 * ```
 *
 * Returned paths are always relative to the storage root and use forward
 * slashes regardless of platform.
 */
class JournalPathResolver
    @Inject
    constructor() {
        fun entryDirectory(date: LocalDate): String = "Journal/${date.year}/${date.monthValue.pad2()}/${date.dayOfMonth.pad2()}"

        fun metadataFile(date: LocalDate): String = "${entryDirectory(date)}/metadata.json"

        fun markdownFile(date: LocalDate): String = "${entryDirectory(date)}/journal.md"

        fun canvasFile(date: LocalDate): String = "${entryDirectory(date)}/canvas.json"

        fun imagesDirectory(date: LocalDate): String = "${entryDirectory(date)}/images"

        fun attachmentsDirectory(date: LocalDate): String = "${entryDirectory(date)}/attachments"

        private fun Int.pad2(): String = toString().padStart(2, '0')
    }

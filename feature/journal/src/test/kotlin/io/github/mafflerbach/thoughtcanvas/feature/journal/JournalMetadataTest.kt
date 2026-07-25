package io.github.mafflerbach.thoughtcanvas.feature.journal

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalMetadataTest {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `round trips through JSON`() {
        val original = JournalMetadata(
            date = "2025-01-17",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_100_000L,
            tags = listOf("work", "ideas"),
        )

        val encoded = json.encodeToString(JournalMetadata.serializer(), original)
        val decoded = json.decodeFromString(JournalMetadata.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `ignores unknown keys for forward compatibility`() {
        // Simulate a metadata file written by a newer app version.
        val forward =
            """
            {
              "schemaVersion": 2,
              "date": "2025-01-17",
              "createdAt": 1,
              "updatedAt": 2,
              "tags": ["a"],
              "someFutureField": "hello"
            }
            """.trimIndent()

        val lenient = Json { ignoreUnknownKeys = true }
        val decoded = lenient.decodeFromString(JournalMetadata.serializer(), forward)

        assertEquals("2025-01-17", decoded.date)
        assertEquals(listOf("a"), decoded.tags)
    }

    @Test
    fun `defaults schemaVersion when absent`() {
        val minimal =
            """
            {"date":"2025-01-17","createdAt":1,"updatedAt":2}
            """.trimIndent()
        val decoded = Json.decodeFromString(JournalMetadata.serializer(), minimal)
        assertEquals(JournalMetadata.SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(emptyList<String>(), decoded.tags)
    }
}

package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class JournalIndexRepositoryTest {
    private lateinit var db: ThoughtCanvasDatabase
    private lateinit var repository: JournalIndexRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room
            .inMemoryDatabaseBuilder(context, ThoughtCanvasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = JournalIndexRepository(
            db = db,
            entryDao = db.journalEntryDao(),
            tagDao = db.tagDao(),
            attachmentDao = db.attachmentDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertEntry_roundTrips() = runTest {
        val today = LocalDate.of(2025, 1, 17)
        repository.upsertEntry(today, updatedAt = 100L, createdAt = 100L)

        val entry = repository.observeEntry(today).first()
        assertNotNull(entry)
        assertEquals("2025-01-17", entry!!.date)
        assertEquals(100L, entry.updatedAt)
        assertEquals(100L, entry.createdAt)
    }

    @Test
    fun upsertEntry_preservesCreatedAt() = runTest {
        val today = LocalDate.of(2025, 1, 17)
        repository.upsertEntry(today, updatedAt = 100L, createdAt = 100L)
        repository.upsertEntry(today, updatedAt = 200L)

        val entry = repository.observeEntry(today).first()!!
        assertEquals(100L, entry.createdAt)
        assertEquals(200L, entry.updatedAt)
    }

    @Test
    fun setTags_replacesPreviousTags() = runTest {
        val today = LocalDate.of(2025, 1, 17)
        repository.upsertEntry(today, updatedAt = 1L, createdAt = 1L)

        repository.setTags(today, listOf("work", "ideas"))
        val first = repository.observeTags(today).first().map { it.name }
        assertEquals(listOf("ideas", "work"), first)

        repository.setTags(today, listOf("personal"))
        val second = repository.observeTags(today).first().map { it.name }
        assertEquals(listOf("personal"), second)
    }

    @Test
    fun setTags_deduplicatesTagsAcrossEntries() = runTest {
        val monday = LocalDate.of(2025, 1, 13)
        val tuesday = LocalDate.of(2025, 1, 14)
        repository.upsertEntry(monday, 1L, 1L)
        repository.upsertEntry(tuesday, 1L, 1L)

        repository.setTags(monday, listOf("work"))
        repository.setTags(tuesday, listOf("work"))

        val allTags = repository.observeAllTags().first()
        assertEquals(1, allTags.size)
        assertEquals("work", allTags.single().name)
    }

    @Test
    fun deleteEntry_cascadesToTagsAndAttachments() = runTest {
        val today = LocalDate.of(2025, 1, 17)
        repository.upsertEntry(today, 1L, 1L)
        repository.setTags(today, listOf("work"))
        repository.addAttachment(today, kind = "image", relativePath = "images/a.jpg", createdAt = 1L)

        repository.deleteEntry(today)

        assertNull(repository.observeEntry(today).first())
        assertEquals(emptyList<Any>(), repository.observeTags(today).first())
        assertEquals(emptyList<Any>(), repository.observeAttachments(today).first())
    }

    @Test
    fun addAttachment_isObservablePerEntry() = runTest {
        val today = LocalDate.of(2025, 1, 17)
        repository.upsertEntry(today, 1L, 1L)

        repository.addAttachment(today, "image", "images/a.jpg", 1L)
        repository.addAttachment(today, "image", "images/b.jpg", 2L)

        val list = repository.observeAttachments(today).first()
        assertEquals(2, list.size)
        assertEquals(listOf("images/a.jpg", "images/b.jpg"), list.map { it.relativePath })
    }
}

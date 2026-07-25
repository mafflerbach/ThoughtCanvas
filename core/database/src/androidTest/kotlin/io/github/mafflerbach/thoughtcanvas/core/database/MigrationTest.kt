package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration harness. Empty for now — schema is at version 1. Any future
 * migration must add a `startMigrations(N-1, N)` test alongside the actual
 * `Migration` object registered on the database builder.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ThoughtCanvasDatabase::class.java,
    )

    @Test
    fun schemaAtVersion1_opensCleanly() {
        helper.createDatabase(TEST_DB, version = 1).apply { close() }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}

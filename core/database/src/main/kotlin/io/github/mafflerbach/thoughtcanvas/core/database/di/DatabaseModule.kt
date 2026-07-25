package io.github.mafflerbach.thoughtcanvas.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mafflerbach.thoughtcanvas.core.database.ThoughtCanvasDatabase
import io.github.mafflerbach.thoughtcanvas.core.database.dao.AttachmentDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.BlockDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasTagDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.EdgeDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.JournalEntryDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.TagDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ThoughtCanvasDatabase =
        Room
            .databaseBuilder(
                context,
                ThoughtCanvasDatabase::class.java,
                ThoughtCanvasDatabase.DATABASE_NAME,
            )
            // Data-loss migration accepted per ADR-0004 (v1 → v2 block canvas).
            // Remove once Phase 2 has real migrations and user data to preserve.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    // Legacy (v1) DAOs — kept until Phase 2.6 retires the flat journal UI.
    @Provides
    fun provideJournalEntryDao(db: ThoughtCanvasDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides
    fun provideAttachmentDao(db: ThoughtCanvasDatabase): AttachmentDao = db.attachmentDao()

    // Shared.
    @Provides
    fun provideTagDao(db: ThoughtCanvasDatabase): TagDao = db.tagDao()

    // Phase 2 canvas DAOs.
    @Provides
    fun provideCanvasDao(db: ThoughtCanvasDatabase): CanvasDao = db.canvasDao()

    @Provides
    fun provideBlockDao(db: ThoughtCanvasDatabase): BlockDao = db.blockDao()

    @Provides
    fun provideEdgeDao(db: ThoughtCanvasDatabase): EdgeDao = db.edgeDao()

    @Provides
    fun provideCanvasTagDao(db: ThoughtCanvasDatabase): CanvasTagDao = db.canvasTagDao()
}

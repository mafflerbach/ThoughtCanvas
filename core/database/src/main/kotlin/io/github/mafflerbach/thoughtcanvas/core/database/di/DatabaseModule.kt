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
            ).build()

    @Provides
    fun provideJournalEntryDao(db: ThoughtCanvasDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides
    fun provideTagDao(db: ThoughtCanvasDatabase): TagDao = db.tagDao()

    @Provides
    fun provideAttachmentDao(db: ThoughtCanvasDatabase): AttachmentDao = db.attachmentDao()
}

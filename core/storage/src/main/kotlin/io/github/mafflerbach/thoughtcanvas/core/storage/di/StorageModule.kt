package io.github.mafflerbach.thoughtcanvas.core.storage.di

import android.content.Context
import androidx.core.net.toUri
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRoot
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRootPreferences
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRootState
import io.github.mafflerbach.thoughtcanvas.core.storage.saf.SafFileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideStorageRootPreferences(
        @ApplicationContext context: Context,
    ): StorageRootPreferences = StorageRootPreferences(context)

    @Provides
    @Singleton
    fun provideStorageRootState(
        preferences: StorageRootPreferences,
        @ApplicationScope scope: CoroutineScope,
    ): StorageRootState = StorageRootState(preferences, scope)

    @Provides
    @Singleton
    fun provideFileRepository(
        @ApplicationContext context: Context,
        rootState: StorageRootState,
    ): FileRepository =
        SafFileRepository(
            context = context,
            rootUriProvider = {
                (rootState.state.value as? StorageRoot.Configured)?.uriString?.toUri()
            },
            ioDispatcher = Dispatchers.IO,
        )
}

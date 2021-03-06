package org.keynote.godtools.android.db.dagger

import android.content.Context
import dagger.Module
import dagger.Provides
import org.keynote.godtools.android.db.GodToolsDao
import org.keynote.godtools.android.db.GodToolsDatabase
import javax.inject.Singleton

@Module
abstract class DatabaseModule {
    companion object {
        @Provides
        @Singleton
        fun provideGodToolsDao(context: Context) = GodToolsDao.getInstance(context)

        @Provides
        @Singleton
        fun provideGodToolsDatabase(context: Context) = GodToolsDatabase.getInstance(context)
    }
}

package id.rona.app.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.rona.app.data.db.RonaDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRonaDatabase(
        @ApplicationContext context: Context,
        factory: RonaDatabase.Factory,
    ): RonaDatabase = factory.create(context)
}

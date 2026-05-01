package com.freshtrack.di

import android.content.Context
import androidx.room.Room
import com.freshtrack.data.db.FreshTrackDatabase
import com.freshtrack.data.db.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FreshTrackDatabase =
        Room.databaseBuilder(context, FreshTrackDatabase::class.java, "freshtrack.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductDao(db: FreshTrackDatabase): ProductDao = db.productDao()
}

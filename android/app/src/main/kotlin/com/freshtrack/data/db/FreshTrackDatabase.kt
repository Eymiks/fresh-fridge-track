package com.freshtrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class FreshTrackDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}

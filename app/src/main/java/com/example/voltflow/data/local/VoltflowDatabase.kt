package com.example.voltflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        BillEntity::class,
        NotificationEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class VoltflowDatabase : RoomDatabase() {
    abstract fun dao(): VoltflowDao

    companion object {
        fun create(context: Context): VoltflowDatabase =
            Room.databaseBuilder(context, VoltflowDatabase::class.java, "voltflow_cache.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

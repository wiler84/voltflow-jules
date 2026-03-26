package com.example.voltflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VoltflowDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWallet(userId: String): WalletEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentTransactions(userId: String, limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getBills(userId: String): List<BillEntity>

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getNotifications(userId: String): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallet(entity: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(entities: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBills(entities: List<BillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(entities: List<NotificationEntity>)
}

package com.example.voltflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val location: String?,
    val avatarUrl: String?,
    val darkMode: Boolean,
    val accountStatus: String,
    val updatedAt: String?,
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val userId: String,
    val balance: Double,
    val updatedAt: String?,
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val kind: String,
    val utilityType: String,
    val amount: Double,
    val status: String,
    val meterNumber: String?,
    val paymentMethod: String,
    val occurredAt: String?,
    val createdAt: String?,
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amountDue: Double,
    val dueDate: String,
    val status: String,
    val createdAt: String?,
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String?,
)

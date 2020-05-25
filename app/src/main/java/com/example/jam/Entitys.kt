package com.example.jam

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.security.PublicKey
import java.sql.Time
import java.sql.Timestamp
import java.util.*

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "first_name") var firstName: String,
    @ColumnInfo(name = "last_name") var lastName: String?,
    @ColumnInfo(name = "publ_key") var publicKey: PublicKey
)
//TODO add converter for public key

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["senderId"]

    ) var senderId: Int,
    @ColumnInfo(name = "chat_id") var chatId: Int,
    @ColumnInfo(name = "text") var text: String,
    @ColumnInfo(name = "time") var time: Date
)
//TODO add converter for time

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "name") var name: String?
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "user_id") var userId: Int,
    @ColumnInfo(name = "chat_id") var chatId: Int
)

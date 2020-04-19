package com.example.jam

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface jamDao{

//    Users:

    @Query("SELECT * FROM users WHERE id = (:userId)")
    fun getUserById(userId: Int): User

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(users: User)

//    Messages
    @Query("SELECT * FROM messages WHERE chat_id = (:chatId) ORDER BY time")
    fun getAllChatMessages(chatId: Int): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE id = (:messageId)")
    fun getMessageById(messageId: Int): Message

    @Insert
    fun insertAll(vararg messages: Message)

    @Delete
    fun delete(message: Message)

//    Chats
    @Query("SELECT * FROM chats ORDER BY name ASC")
    fun getAllChatAlphabet(): LiveData<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = (:chatId)")
    fun getChatById(chatId: Int): Chat

    @Insert
    fun insertAll(vararg chats: Chat)

    @Delete
    fun delete(chat: Chat)

//    Members

    @Query("SELECT * FROM members WHERE id = (:memberId)")
    fun getMemberById(memberId: Int): Member

    @Insert
    fun insertAll(vararg members: Member)

    @Delete
    fun delete(member: Member)
}
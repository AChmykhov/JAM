package com.example.jam

import androidx.lifecycle.LiveData

class jamRepository(private val jamDao: jamDao) {
    val allChats: LiveData<List<Chat>> = jamDao.getAllChatAlphabet()

    suspend fun insertUser(user: User){
        jamDao.insertAll(user)
       jamDao.insertAll(Chat(name="${user.firstName} ${user.lastName}"))
    }


    fun getMessagesByChat(chat: Chat): LiveData<List<Message>> {
        return jamDao.getAllChatMessages(chat.id)
    }


}
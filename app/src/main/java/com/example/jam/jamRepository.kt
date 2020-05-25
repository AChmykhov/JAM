package com.example.jam

import androidx.lifecycle.LiveData

class jamRepository(private val jamDao: jamDao) {
    val allChats: LiveData<List<Chat>> = jamDao.getAllChatAlphabet()
    //val allMessage: LiveData<List<Message>>

    fun insertUser(user: User) {
        jamDao.insertAll(user)
        jamDao.insertAll(Chat(name = "${user.firstName} ${user.lastName}", id = 0))
    }

    fun insertMessage(message: Message) {
        jamDao.insertAll(message)
    }


    fun getMessagesByChat(chat: Chat): LiveData<List<Message>> {
        return jamDao.getAllChatMessages(chat.id)
    }


}
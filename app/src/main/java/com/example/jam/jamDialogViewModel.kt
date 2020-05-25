package com.example.jam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class jamDialogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: jamRepository
    val allMsgs: MutableLiveData<List<Message>> = TODO()

    init {
        val jamDao = AppDatabase.getDatabase(application).jamDao()
        repository = jamRepository(jamDao)
//        allMsgs = repository.allChats
    }

    fun getMessages(chat: Chat) = viewModelScope.launch(Dispatchers.IO) {
        allMsgs.postValue(repository.getMessagesByChat(chat))
        //TODO: resolve types mismatch with livedata
    }

    fun insertMessage(message: Message) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertMessage(message)
    }

}

/*
FIXME add observer in oncreate in dialog activity
 */
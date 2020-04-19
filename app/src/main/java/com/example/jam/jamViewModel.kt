package com.example.jam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class jamViewModel(application: Application): AndroidViewModel(application) {
    private val repository: jamRepository
    val allChats: LiveData<List<Chat>>
    init {
        val jamDao = AppDatabase.getDatabase(application).jamDao()
        repository = jamRepository(jamDao)
        allChats = repository.allChats
    }
    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertUser(user)
    }

}
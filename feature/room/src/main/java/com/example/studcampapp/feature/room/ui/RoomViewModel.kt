package com.example.studcampapp.feature.room.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studcampapp.data.repository.NsdRepository
import com.example.studcampapp.data.repository.impl.NsdRepositoryImpl
import com.example.studcampapp.feature.room.domain.usecase.JoinRoomUseCase
import kotlinx.coroutines.launch

class RoomViewModel(
    private val joinRoomUseCase: JoinRoomUseCase = JoinRoomUseCase(),
    private val nsdRepository: NsdRepository = NsdRepositoryImpl
) : ViewModel() {

    val discoveredRooms get() = nsdRepository.discoveredRooms

    fun startDiscovery(context: Context) = nsdRepository.startDiscovery(context)
    fun stopDiscovery() = nsdRepository.stopDiscovery()

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var navigateToChat by mutableStateOf(false)
        private set

    fun clearError() { error = null }
    fun onNavigated() { navigateToChat = false }

    fun join(ip: String, port: Int, nickname: String, roomName: String) {
        isLoading = true
        error = null
        viewModelScope.launch {
            joinRoomUseCase(ip, port, nickname, roomName)
                .onSuccess {
                    isLoading = false
                    navigateToChat = true
                }
                .onFailure { e ->
                    error = e.message
                    isLoading = false
                }
        }
    }
}

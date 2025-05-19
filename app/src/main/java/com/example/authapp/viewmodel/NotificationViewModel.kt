package com.example.authapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.Notification
import com.example.authapp.model.NotificationType
import com.example.authapp.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationViewModel"
    private val repository: NotificationRepository = NotificationRepository(application.applicationContext)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _state = MutableStateFlow<NotificationState>(NotificationState.Idle)
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    init {
        Log.d(TAG, "Inicializando NotificationViewModel")
        loadNotifications()
        startListeningForNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = NotificationState.Loading
            repository.getNotificationsForCurrentUser()
                .catch { e ->
                    Log.e(TAG, "Error al cargar notificaciones", e)
                    _state.value = NotificationState.Error(e.message ?: "Error loading notifications")
                }
                .collect { notificationsList ->
                    Log.d(TAG, "Notificaciones cargadas: ${notificationsList.size}")
                    _notifications.value = notificationsList
                    _state.value = NotificationState.Success
                }
        }
    }

    fun startListeningForNotifications() {
        Log.d(TAG, "Iniciando escucha de notificaciones")
        repository.listenForNewNotifications()
    }

    fun stopListeningForNotifications() {
        Log.d(TAG, "Deteniendo escucha de notificaciones")
        repository.stopAllListeners()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            _state.value = NotificationState.Loading
            repository.markNotificationAsRead(notificationId).fold(
                onSuccess = {
                    // Recargar notificaciones para actualizar la UI
                    loadNotifications()
                },
                onFailure = { error ->
                    Log.e(TAG, "Error al marcar como leída", error)
                    _state.value = NotificationState.Error(error.message ?: "Error marking notification as read")
                }
            )
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            _state.value = NotificationState.Loading
            repository.deleteNotification(notificationId).fold(
                onSuccess = {
                    // Recargar notificaciones para actualizar la UI
                    loadNotifications()
                },
                onFailure = { error ->
                    Log.e(TAG, "Error al eliminar notificación", error)
                    _state.value = NotificationState.Error(error.message ?: "Error deleting notification")
                }
            )
        }
    }

    fun createNotification(userId: String, title: String, message: String, type: NotificationType) {
        viewModelScope.launch {
            _state.value = NotificationState.Loading
            repository.createNotification(userId, title, message, type).fold(
                onSuccess = {
                    // Recargar notificaciones para actualizar la UI
                    loadNotifications()
                },
                onFailure = { error ->
                    Log.e(TAG, "Error al crear notificación", error)
                    _state.value = NotificationState.Error(error.message ?: "Error creating notification")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForNotifications()
    }

    sealed class NotificationState {
        object Idle : NotificationState()
        object Loading : NotificationState()
        object Success : NotificationState()
        data class Error(val message: String) : NotificationState()
    }
}
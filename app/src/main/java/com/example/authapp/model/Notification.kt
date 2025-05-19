package com.example.authapp.model

import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = Date().time,
    val read: Boolean = false,
    val type: NotificationType = NotificationType.INFO
) {
    // Constructor vacío para Firebase
    constructor() : this("", "", "", "", Date().time, false, NotificationType.INFO)
}

enum class NotificationType {
    INFO, WARNING, ERROR, SUCCESS
}
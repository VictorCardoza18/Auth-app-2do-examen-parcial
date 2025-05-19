package com.example.authapp.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.authapp.MainActivity
import com.example.authapp.R
import com.example.authapp.model.Notification
import com.example.authapp.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class NotificationRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val TAG = "NotificationRepository"

    // Referencia a los child event listeners para poder eliminarlos después
    private var notificationListeners = mutableListOf<Pair<Query, ChildEventListener>>()

    // Obtener notificaciones para el usuario actual - CORREGIDO
    fun getNotificationsForCurrentUser(): Flow<List<Notification>> = callbackFlow {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        // Esta es la consulta importante para obtener todas las notificaciones del usuario actual
        val notificationsRef = database.child("notifications").orderByChild("userId").equalTo(currentUser.uid)

        val listener = notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<Notification>()
                for (notificationSnapshot in snapshot.children) {
                    try {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        if (notification != null) {
                            Log.d(TAG, "Notificación recuperada: ${notification.title}, leída: ${notification.read}")
                            notifications.add(notification)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al convertir notificación", e)
                    }
                }

                // Ordenar por timestamp (más reciente primero)
                val orderedNotifications = notifications.sortedByDescending { it.timestamp }
                trySend(orderedNotifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener notificaciones", error.toException())
                close(error.toException())
            }
        })

        awaitClose {
            notificationsRef.removeEventListener(listener)
        }
    }

    // Escuchar por nuevas notificaciones no leídas para el usuario actual
    fun listenForNewNotifications() {
        // Primero, detener cualquier listener previo
        stopAllListeners()

        val currentUser = auth.currentUser ?: return

        // Consulta para obtener notificaciones específicas para este usuario que no han sido leídas
        val notificationsRef = database.child("notifications")
            .orderByChild("userId")
            .equalTo(currentUser.uid)

        val childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val notification = snapshot.getValue(Notification::class.java)

                    // Solo mostrar notificaciones push si no han sido leídas y pertenecen al usuario actual
                    if (notification != null && !notification.read && notification.userId == currentUser.uid) {
                        Log.d(TAG, "Nueva notificación para ${currentUser.uid}: ${notification.title}")
                        showNotificationPush(notification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar notificación", e)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos acción aquí
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // No necesitamos acción aquí
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos acción aquí
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al escuchar notificaciones", error.toException())
            }
        }

        notificationsRef.addChildEventListener(childListener)

        // Guardar la referencia para poder detenerla más tarde
        notificationListeners.add(Pair(notificationsRef, childListener))
    }

    // Detener todos los listeners de notificaciones
    fun stopAllListeners() {
        notificationListeners.forEach { (query, listener) ->
            query.removeEventListener(listener)
        }
        notificationListeners.clear()
    }

    // Crear una nueva notificación para un usuario específico
    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType
    ): Result<Notification> {
        return try {
            val notificationId = UUID.randomUUID().toString()
            val notification = Notification(
                id = notificationId,
                userId = userId,
                title = title,
                message = message,
                timestamp = Date().time,
                read = false,
                type = type
            )

            Log.d(TAG, "Creando notificación: $title para usuario $userId")
            database.child("notifications").child(notificationId).setValue(notification).await()
            Result.success(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear notificación", e)
            Result.failure(e)
        }
    }

    // Marcar una notificación como leída
    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Marcando notificación como leída: $notificationId")
            database.child("notifications").child(notificationId).child("read").setValue(true).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al marcar notificación como leída", e)
            Result.failure(e)
        }
    }

    // Eliminar una notificación
    suspend fun deleteNotification(notificationId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Eliminando notificación: $notificationId")
            database.child("notifications").child(notificationId).removeValue().await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar notificación", e)
            Result.failure(e)
        }
    }

    // Mostrar notificación push
    private fun showNotificationPush(notification: Notification) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Determinar el color según el tipo
        val color = when (notification.type) {
            NotificationType.INFO -> 0xFF2196F3.toInt() // Azul
            NotificationType.WARNING -> 0xFFFFC107.toInt() // Amarillo
            NotificationType.ERROR -> 0xFFF44336.toInt() // Rojo
            NotificationType.SUCCESS -> 0xFF4CAF50.toInt() // Verde
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android Oreo, es necesario crear un canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Canal de notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Usar el id de la notificación para que cada notificación sea única
        val notificationId = notification.id.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
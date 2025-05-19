package com.example.authapp.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.authapp.model.Notification
import com.example.authapp.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class NotificationService : Service() {
    private val TAG = "NotificationService"

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private var notificationsListener: ChildEventListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startListeningForNotifications()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si el servicio se reinicia, volver a iniciar la escucha
        if (notificationsListener == null) {
            startListeningForNotifications()
        }

        return START_STICKY
    }

    private fun startListeningForNotifications() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Escuchar solo las notificaciones del usuario actual
            val notificationsRef = database.child("notifications")
                .orderByChild("userId")
                .equalTo(currentUser.uid)

            notificationsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    try {
                        val notification = snapshot.getValue(Notification::class.java)

                        // Solo mostrar notificaciones no leídas
                        if (notification != null && !notification.read) {
                            Log.d(TAG, "Nueva notificación no leída: ${notification.title}")

                            // Crear notificación push
                            val messagingService = MyFirebaseMessagingService()
                            messagingService.sendNotification(
                                notification.title,
                                notification.message,
                                notification.type.name
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar notificación", e)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // No necesitamos hacer nada aquí
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // No necesitamos hacer nada aquí
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // No necesitamos hacer nada aquí
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error en la escucha de notificaciones", error.toException())
                }
            }

            notificationsRef.addChildEventListener(notificationsListener!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Eliminar la escucha al destruir el servicio
        notificationsListener?.let {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val notificationsRef = database.child("notifications")
                    .orderByChild("userId")
                    .equalTo(currentUser.uid)

                notificationsRef.removeEventListener(it)
            }
            notificationsListener = null
        }
    }
}
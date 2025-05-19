package com.example.authapp.notifications

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMessagingService"

    override fun onNewToken(token: String) {
        Log.d(TAG, "New token: $token")
        // Aquí deberías enviar el token al servidor para asociarlo con el usuario
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Si tienes datos en el mensaje, los puedes procesar aquí
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val title = remoteMessage.data["title"] ?: "Nueva notificación"
            val message = remoteMessage.data["message"] ?: ""
            val type = remoteMessage.data["type"] ?: NotificationType.INFO.name

            sendNotification(title, message, type)
        }

        // Si tienes un mensaje de notificación, puedes personalizarlo
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Nueva notificación", it.body ?: "", NotificationType.INFO.name)
        }
    }

    public fun sendNotification(title: String, messageBody: String, typeStr: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Determinar el color y prioridad según el tipo
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (e: Exception) {
            NotificationType.INFO
        }

        val color = when (type) {
            NotificationType.INFO -> 0xFF2196F3.toInt() // Azul
            NotificationType.WARNING -> 0xFFFFC107.toInt() // Amarillo
            NotificationType.ERROR -> 0xFFF44336.toInt() // Rojo
            NotificationType.SUCCESS -> 0xFF4CAF50.toInt() // Verde
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun sendRegistrationToServer(token: String) {
        // Implementar función para enviar el token al servidor
        // Esto es importante si quieres enviar notificaciones específicas a este dispositivo
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().reference
            database.child("users").child(currentUser.uid).child("fcmToken").setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token guardado exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar token", e)
                }
        }
    }
}
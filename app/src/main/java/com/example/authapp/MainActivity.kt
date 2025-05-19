package com.example.authapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.authapp.navigation.AppNavigation
import com.example.authapp.ui.theme.AuthAppTheme
import com.example.authapp.viewmodel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private lateinit var auth: FirebaseAuth

    // Solicitar permiso para notificaciones en Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Permiso de notificaciones concedido")
            setupNotificationListener()
        } else {
            Log.d(TAG, "Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewModel
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configurar AuthStateListener para manejar cambios de sesión
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Usuario conectado, iniciar escucha de notificaciones
                Log.d(TAG, "Usuario conectado: ${user.uid}")
                setupNotificationListener()
            } else {
                // Usuario desconectado, detener escucha de notificaciones
                Log.d(TAG, "Usuario desconectado")
                notificationViewModel.stopListeningForNotifications()
            }
        }

        // Solicitar token de FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            saveTokenToDatabase(token)
        }

        // Solicitar permisos de notificación para Android 13+
        requestNotificationPermission()

        setContent {
            AuthAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(notificationViewModel = notificationViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Agregar listener cuando la actividad inicia
        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        // Eliminar listener cuando la actividad se detiene
        auth.removeAuthStateListener(authListener)
    }

    private fun setupNotificationListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationViewModel.startListeningForNotifications()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            notificationViewModel.startListeningForNotifications()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tenemos el permiso
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explicar al usuario por qué necesitamos el permiso
                    // Luego solicitar el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Solicitar el permiso directamente
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun saveTokenToDatabase(token: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
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
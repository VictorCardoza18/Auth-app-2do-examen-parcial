package com.example.authapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.authapp.model.NotificationType
import com.example.authapp.model.User
import com.example.authapp.viewmodel.AuthViewModel
import com.example.authapp.viewmodel.NotificationViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onSignOut: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Estado para los usuarios
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Estado para las notificaciones
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedNotificationType by remember { mutableStateOf(NotificationType.INFO) }

    // Cargamos los usuarios al inicio
    LaunchedEffect(Unit) {
        isLoadingUsers = true
        val database = FirebaseDatabase.getInstance().reference
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let { userList.add(it) }
                }
                users = userList
                isLoadingUsers = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingUsers = false
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administrador") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "¡Bienvenido, Admin ${currentUser?.name ?: ""}!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Rol: Administrador",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Email: ${currentUser?.email ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            // Panel para enviar notificaciones
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Enviar Notificación",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Título de la notificación
                        OutlinedTextField(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = { Text("Título") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )

                        // Mensaje de la notificación
                        OutlinedTextField(
                            value = notificationMessage,
                            onValueChange = { notificationMessage = it },
                            label = { Text("Mensaje") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            minLines = 3,
                            maxLines = 5
                        )

                        // Tipo de notificación
                        Text(
                            text = "Tipo de notificación:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NotificationType.values().forEach { type ->
                                FilterChip(
                                    selected = selectedNotificationType == type,
                                    onClick = { selectedNotificationType = type },
                                    label = {
                                        Text(
                                            when(type) {
                                                NotificationType.INFO -> "Info"
                                                NotificationType.WARNING -> "Advertencia"
                                                NotificationType.ERROR -> "Error"
                                                NotificationType.SUCCESS -> "Éxito"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        // Selección de usuario
                        Text(
                            text = "Enviar a:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (isLoadingUsers) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedUserId == null,
                                    onClick = { selectedUserId = null },
                                    label = { Text("Todos los usuarios") }
                                )

                                users.forEach { user ->
                                    if (!user.admin) {  // No mostrar administradores
                                        FilterChip(
                                            selected = selectedUserId == user.uid,
                                            onClick = { selectedUserId = user.uid },
                                            label = { Text(user.name) }
                                        )
                                    }
                                }
                            }
                        }

                        // Botón para enviar notificación
                        Button(
                            onClick = {
                                if (notificationTitle.isNotBlank() && notificationMessage.isNotBlank()) {
                                    coroutineScope.launch {
                                        if (selectedUserId != null) {
                                            // Enviar a un usuario específico
                                            notificationViewModel.createNotification(
                                                userId = selectedUserId!!,
                                                title = notificationTitle,
                                                message = notificationMessage,
                                                type = selectedNotificationType
                                            )
                                        } else {
                                            // Enviar a todos los usuarios no administradores
                                            users.forEach { user ->
                                                if (!user.admin) {
                                                    notificationViewModel.createNotification(
                                                        userId = user.uid,
                                                        title = notificationTitle,
                                                        message = notificationMessage,
                                                        type = selectedNotificationType
                                                    )
                                                }
                                            }
                                        }

                                        // Limpiar campos después de enviar
                                        notificationTitle = ""
                                        notificationMessage = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = notificationTitle.isNotBlank() && notificationMessage.isNotBlank()
                        ) {
                            Text("Enviar Notificación")
                        }
                    }
                }
            }

            // Lista de usuarios
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Usuarios Registrados",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (isLoadingUsers) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                            )
                        } else if (users.isEmpty()) {
                            Text(
                                text = "No hay usuarios registrados",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            users.forEach { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = if (user.admin) "Administrador" else "Usuario normal",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (user.admin)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (!user.admin) {
                                            Button(
                                                onClick = {
                                                    selectedUserId = user.uid
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                Text("Notificar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Cerrar Sesión")
                }
            }
        }
    }
}
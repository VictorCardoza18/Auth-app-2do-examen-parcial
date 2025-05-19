package com.example.authapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.User
import com.example.authapp.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUser()?.let {
                repository.getCurrentUserData().fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated
                        // Añadimos un log para depuración
                        println("Usuario autenticado: ${user.name}, Es administrador: ${user.admin}")
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error al recuperar datos del usuario")
                        // Añadimos un log para depuración
                        println("Error al obtener datos de usuario: ${error.message}")
                    }
                )
            } ?: run {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun registerUser(email: String, password: String, name: String, isAdmin: Boolean, masterPassword: String? = null) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.createUser(email, password, name, isAdmin, masterPassword).fold(
                onSuccess = {
                    // Después de crear el usuario, necesitamos asegurarnos de obtener sus datos completos
                    repository.getCurrentUserData().fold(
                        onSuccess = { user ->
                            _currentUser.value = user
                            _authState.value = AuthState.Authenticated
                            // Añadimos un log para depuración
                            println("Usuario registrado: ${user.name}, Es administrador: ${user.admin}")
                        },
                        onFailure = { error ->
                            _authState.value = AuthState.Error(error.message ?: "Error al recuperar datos del usuario")
                        }
                    )
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registro fallido")
                }
            )
        }
    }

    fun loginUser(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.loginUser(email, password).fold(
                onSuccess = {
                    // Después de iniciar sesión, necesitamos asegurarnos de obtener los datos completos del usuario
                    repository.getCurrentUserData().fold(
                        onSuccess = { user ->
                            _currentUser.value = user
                            _authState.value = AuthState.Authenticated
                            // Añadimos un log para depuración
                            println("Usuario conectado: ${user.name}, Es administrador: ${user.admin}")
                        },
                        onFailure = { error ->
                            _authState.value = AuthState.Error(error.message ?: "Error al recuperar datos del usuario")
                        }
                    )
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Inicio de sesión fallido")
                }
            )
        }
    }

    fun signOut() {
        repository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Authenticated : AuthState()
        object Unauthenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
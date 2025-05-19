package com.example.authapp.repository

import com.example.authapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Master password for admin creation
    companion object {
        const val ADMIN_MASTER_PASSWORD = "admin123" // Change this to your secure password
    }

    suspend fun createUser(email: String, password: String, name: String, isAdmin: Boolean, masterPassword: String? = null): Result<FirebaseUser> {
        return try {
            // Verify master password for admin creation
            if (isAdmin && masterPassword != ADMIN_MASTER_PASSWORD) {
                return Result.failure(Exception("Contraseña maestra inválida"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            authResult.user?.let { firebaseUser ->
                // Save user data to Realtime Database
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    name = name,
                    admin = isAdmin
                )
                database.child("users").child(firebaseUser.uid).setValue(user).await()
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("La creación del usuario falló"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let { firebaseUser ->
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("Inicio de sesión fallido"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserData(): Result<User> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No hay usuario conectado"))

        return suspendCoroutine { continuation ->
            database.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { snapshot ->
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resume(Result.failure(Exception("Datos de usuario no encontrados")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}
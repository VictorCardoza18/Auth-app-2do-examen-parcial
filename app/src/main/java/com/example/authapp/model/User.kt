package com.example.authapp.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val admin: Boolean = false
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", false)
}
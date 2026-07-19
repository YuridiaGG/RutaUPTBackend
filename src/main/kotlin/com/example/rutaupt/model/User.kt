package com.example.rutaupt.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int? = null,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val password: String? = null,
    val rol: String, // admin, chofer, estudiante
    val numeroUnidad: String? = null,
    val edad: String? = null,
    val telefono: String? = null,
    val horario: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val pass: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class RecoveryRequest(
    val email: String
)

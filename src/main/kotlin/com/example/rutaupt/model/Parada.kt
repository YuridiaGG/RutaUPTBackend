package com.example.rutaupt.model

import kotlinx.serialization.Serializable

@Serializable
data class Parada(
    val id: Int? = null,
    val nombre: String,
    val ubicacion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

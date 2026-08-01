package com.example.rutaupt.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
enum class ReporteTipo {
    ALERTA, INFORMACION, RETRASO
}

@Serializable
data class ReporteUnidad(
    val id: Long? = null,
    val unidad: String,
    val mensaje: String,
    val tiempo: String,
    val tipo: ReporteTipo,
    val imagen: String? = null,
    val estado: String? = null,
    var validacionAdmin: String? = null
)

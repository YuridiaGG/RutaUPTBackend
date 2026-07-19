package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.ReporteUnidad
import com.example.rutaupt.model.ReporteTipo
import org.jetbrains.exposed.sql.*

class ReportesRepository {
    suspend fun getAllReportes(): List<ReporteUnidad> = dbQuery {
        Reportes.selectAll().map { rowToReporte(it) }
    }

    suspend fun addReporte(reporte: ReporteUnidad): Boolean = dbQuery {
        try {
            Reportes.insert {
                it[unidad] = reporte.unidad
                it[mensaje] = reporte.mensaje
                it[fechaHora] = reporte.tiempo
                it[tipo] = reporte.tipo.name
                it[imagen] = reporte.imagen
                it[estado] = reporte.estado ?: "PENDIENTE"
                it[validacionAdmin] = reporte.validacionAdmin
            }.insertedCount > 0
        } catch (e: Exception) {
            println("Error al insertar reporte: ${e.message}")
            false
        }
    }

    private fun rowToReporte(row: ResultRow) = ReporteUnidad(
        id = row[Reportes.id],
        unidad = row[Reportes.unidad],
        mensaje = row[Reportes.mensaje],
        tiempo = row[Reportes.fechaHora],
        tipo = ReporteTipo.valueOf(row[Reportes.tipo]),
        imagen = row[Reportes.imagen],
        estado = row[Reportes.estado],
        validacionAdmin = row[Reportes.validacionAdmin]
    )
}

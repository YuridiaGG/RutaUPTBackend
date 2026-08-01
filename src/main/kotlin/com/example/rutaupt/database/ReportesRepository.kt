package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.ReporteUnidad
import com.example.rutaupt.model.ReporteTipo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportesRepository {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    suspend fun getAllReportes(): List<ReporteUnidad> = dbQuery {
        Reportes.selectAll()
            .orderBy(Reportes.id to SortOrder.DESC)
            .map { rowToReporte(it) }
    }

    suspend fun addReporte(reporte: ReporteUnidad): Boolean = dbQuery {
        try {
            // Si el tiempo es "reciente", vacío o nulo, forzamos la fecha del servidor
            val fechaParaGuardar = if (reporte.tiempo.isNullOrBlank() || 
                reporte.tiempo.lowercase().contains("reciente")) {
                LocalDateTime.now().format(formatter)
            } else {
                reporte.tiempo
            }

            Reportes.insert {
                it[unidad] = reporte.unidad
                it[mensaje] = reporte.mensaje
                it[fechaHora] = fechaParaGuardar
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

    suspend fun updateReporteEstado(id: Long, nuevoEstado: String, validacion: String?): Boolean = dbQuery {
        Reportes.update({ Reportes.id eq id }) {
            it[estado] = nuevoEstado
            if (validacion != null) {
                it[validacionAdmin] = validacion
            }
        } > 0
    }

    suspend fun deleteReporte(id: Long): Boolean = dbQuery {
        Reportes.deleteWhere { Reportes.id eq id } > 0
    }

    private fun rowToReporte(row: ResultRow) = ReporteUnidad(
        id = row[Reportes.id],
        unidad = row[Reportes.unidad],
        mensaje = row[Reportes.mensaje],
        tiempo = row[Reportes.fechaHora],
        tipo = try { 
            ReporteTipo.valueOf(row[Reportes.tipo]) 
        } catch (e: Exception) { 
            // Manejo de seguridad para tipos nuevos como RETRASO
            if (row[Reportes.tipo] == "RETRASO") ReporteTipo.RETRASO 
            else ReporteTipo.INFORMACION
        },
        imagen = row[Reportes.imagen],
        estado = row[Reportes.estado],
        validacionAdmin = row[Reportes.validacionAdmin]
    )
}

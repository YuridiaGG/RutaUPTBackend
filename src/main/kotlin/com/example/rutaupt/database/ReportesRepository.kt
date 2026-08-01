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

    suspend fun addReporte(reporte: ReporteUnidad): Long? = dbQuery {
        try {
            // Fix Fechas: Si viene "reciente", ponemos la fecha real del servidor
            val fechaReal = if (reporte.tiempo.lowercase().contains("reciente") || reporte.tiempo.isBlank()) {
                LocalDateTime.now().format(formatter)
            } else {
                reporte.tiempo
            }

            val insertedId = Reportes.insert {
                // Si el móvil ya envió un ID (timestamp), lo usamos. Si no, la DB genera uno.
                if (reporte.id != null && reporte.id > 0) {
                    it[id] = reporte.id
                }
                it[unidad] = reporte.unidad
                it[mensaje] = reporte.mensaje
                it[fechaHora] = fechaReal
                it[tipo] = reporte.tipo.name
                it[imagen] = reporte.imagen
                it[estado] = reporte.estado ?: "PENDIENTE"
                it[validacionAdmin] = reporte.validacionAdmin
            } get Reportes.id
            insertedId
        } catch (e: Exception) {
            println("Error al insertar reporte: ${e.message}")
            null
        }
    }

    suspend fun updateReporteEstado(id: Long, nuevoEstado: String, validacion: String?): Boolean = dbQuery {
        try {
            Reportes.update({ Reportes.id eq id }) {
                it[estado] = nuevoEstado
                if (validacion != null) {
                    it[validacionAdmin] = validacion
                }
            } > 0
        } catch (e: Exception) {
            println("Error al actualizar estado del reporte $id: ${e.message}")
            false
        }
    }

    suspend fun deleteReporte(id: Long): Boolean = dbQuery {
        try {
            Reportes.deleteWhere { Reportes.id eq id } > 0
        } catch (e: Exception) {
            println("Error al borrar reporte $id: ${e.message}")
            false
        }
    }

    private fun rowToReporte(row: ResultRow): ReporteUnidad {
        val tipoStr = row[Reportes.tipo].uppercase()
        val tipoEnum = try {
            ReporteTipo.valueOf(tipoStr)
        } catch (e: Exception) {
            when (tipoStr) {
                "RETRASO" -> ReporteTipo.RETRASO
                "ALERTA" -> ReporteTipo.ALERTA
                else -> ReporteTipo.INFORMACION
            }
        }

        return ReporteUnidad(
            id = row[Reportes.id],
            unidad = row[Reportes.unidad],
            mensaje = row[Reportes.mensaje],
            tiempo = row[Reportes.fechaHora],
            tipo = tipoEnum,
            imagen = row[Reportes.imagen],
            estado = row[Reportes.estado],
            validacionAdmin = row[Reportes.validacionAdmin]
        )
    }
}

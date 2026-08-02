package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.Parada
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal

class ParadasRepository {
    suspend fun getAllParadas(): List<Parada> = dbQuery {
        Paradas.selectAll().map { row ->
            Parada(
                id = row[Paradas.id],
                nombre = row[Paradas.nombre],
                ubicacion = row[Paradas.ubicacion],
                latitud = row[Paradas.latitud]?.toDouble(),
                longitud = row[Paradas.longitud]?.toDouble()
            )
        }
    }

    suspend fun addParada(parada: Parada): Int? = dbQuery {
        try {
            val result = Paradas.insert { statement ->
                statement[Paradas.nombre] = parada.nombre
                statement[Paradas.ubicacion] = parada.ubicacion
                statement[Paradas.latitud] = parada.latitud?.let { BigDecimal.valueOf(it) }
                statement[Paradas.longitud] = parada.longitud?.let { BigDecimal.valueOf(it) }
            }
            result[Paradas.id]
        } catch (e: Exception) {
            println("ERROR AL GUARDAR PARADA EN DB: ${e.message}")
            null
        }
    }

    suspend fun deleteParadaById(id: Int): Boolean = dbQuery {
        Paradas.deleteWhere { Paradas.id eq id } > 0
    }
}

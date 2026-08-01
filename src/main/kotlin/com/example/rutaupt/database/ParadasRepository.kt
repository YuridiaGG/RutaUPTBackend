package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class Parada(val id: Int? = null, val nombre: String, val ubicacion: String? = null)

class ParadasRepository {
    suspend fun getAllParadas(): List<Parada> = dbQuery {
        Paradas.selectAll().map { row ->
            Parada(
                id = row[Paradas.id],
                nombre = row[Paradas.nombre],
                ubicacion = row[Paradas.ubicacion]
            )
        }
    }

    suspend fun addParada(nombre: String, ubicacion: String?): Boolean = dbQuery {
        try {
            Paradas.insert {
                it[Paradas.nombre] = nombre
                it[Paradas.ubicacion] = ubicacion
            }.insertedCount > 0
        } catch (e: Exception) {
            println("Error al insertar parada: ${e.message}")
            false
        }
    }

    suspend fun deleteParadaByName(nombre: String): Boolean = dbQuery {
        Paradas.deleteWhere { Paradas.nombre eq nombre } > 0
    }

    suspend fun deleteParadaById(id: Int): Boolean = dbQuery {
        Paradas.deleteWhere { Paradas.id eq id } > 0
    }
}

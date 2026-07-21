package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*

@kotlinx.serialization.Serializable
data class RutaModel(val id: Int? = null, val nombre: String, val color: String, val activa: Boolean = true)

class RutasRepository {
    suspend fun getAllRutas(): List<RutaModel> = dbQuery {
        Rutas.selectAll().map {
            RutaModel(
                id = it[Rutas.idRuta],
                nombre = it[Rutas.nombreRuta],
                color = it[Rutas.color],
                activa = it[Rutas.activa]
            )
        }
    }

    suspend fun addRuta(nombre: String, color: String): Boolean = dbQuery {
        val inserted = Rutas.insert {
            it[Rutas.nombreRuta] = nombre
            it[Rutas.color] = color
            it[Rutas.activa] = true
        }
        inserted.insertedCount > 0
    }

    suspend fun getRutasCount(): Long = dbQuery {
        Rutas.selectAll().count()
    }
}

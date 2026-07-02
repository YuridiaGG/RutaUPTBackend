package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class AuthRepository {
    suspend fun findUserByEmail(email: String): User? = dbQuery {
        Usuarios.selectAll().where { Usuarios.email eq email }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun registerUser(user: User): Boolean = dbQuery {
        try {
            Usuarios.insert {
                it[Usuarios.nombre] = user.nombre
                it[Usuarios.apellidos] = user.apellidos
                it[Usuarios.email] = user.email
                it[Usuarios.password] = user.password ?: ""
                it[Usuarios.rol] = user.rol
                it[Usuarios.edad] = user.edad
                it[Usuarios.telefono] = user.telefono
                it[Usuarios.numeroUnidad] = user.numeroUnidad
            }
            true
        } catch (e: Exception) {
            println("Error al insertar usuario: ${e.message}")
            false
        }
    }

    suspend fun getUserPassword(email: String): String? = dbQuery {
        Usuarios.selectAll().where { Usuarios.email eq email }
            .map { it[Usuarios.password] }
            .singleOrNull()
    }

    // NUEVAS FUNCIONES PARA EL ADMIN
    suspend fun getAllUsersByRol(rol: String): List<User> = dbQuery {
        Usuarios.selectAll().where { Usuarios.rol eq rol }
            .map { rowToUser(it) }
    }

    suspend fun deleteUser(id: Int): Boolean = dbQuery {
        Usuarios.deleteWhere { Usuarios.id eq id } > 0
    }
    
    suspend fun getRoutesCount(): Long = dbQuery {
        Rutas.selectAll().count()
    }

    private fun rowToUser(row: ResultRow) = User(
        id = row[Usuarios.id],
        nombre = row[Usuarios.nombre],
        apellidos = row[Usuarios.apellidos],
        email = row[Usuarios.email],
        rol = row[Usuarios.rol],
        numeroUnidad = row[Usuarios.numeroUnidad],
        edad = row[Usuarios.edad],
        telefono = row[Usuarios.telefono]
    )
}

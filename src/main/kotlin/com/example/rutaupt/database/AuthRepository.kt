package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.User
import kotlinx.datetime.*
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
                it[Usuarios.rol] = user.rol.lowercase()
                it[Usuarios.edad] = user.edad
                it[Usuarios.telefono] = user.telefono
                it[Usuarios.numeroUnidad] = user.numeroUnidad
                it[Usuarios.horario] = user.horario
            }
            true
        } catch (e: Exception) {
            println("Error al insertar usuario: ${e.message}")
            false
        }
    }

    suspend fun updateUser(user: User): Boolean = dbQuery {
        val userId = user.id ?: return@dbQuery false
        Usuarios.update({ Usuarios.id eq userId }) {
            it[Usuarios.nombre] = user.nombre
            it[Usuarios.apellidos] = user.apellidos
            it[Usuarios.email] = user.email
            if (user.password != null) it[Usuarios.password] = user.password
            it[Usuarios.rol] = user.rol.lowercase()
            it[Usuarios.edad] = user.edad
            it[Usuarios.telefono] = user.telefono
            it[Usuarios.numeroUnidad] = user.numeroUnidad
            it[Usuarios.horario] = user.horario
        } > 0
    }

    suspend fun getUserPassword(email: String): String? = dbQuery {
        Usuarios.selectAll().where { Usuarios.email eq email }
            .map { it[Usuarios.password] }
            .singleOrNull()
    }

    // --- MÉTODOS DE RECUPERACIÓN CON CÓDIGO ---

    suspend fun saveRecoveryCode(email: String, code: String): Boolean = dbQuery {
        CodigosRecuperacion.deleteWhere { CodigosRecuperacion.email eq email }
        
        // Ajustado a 10 minutos según el nuevo formato de correo
        val exp = Clock.System.now().plus(10, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.currentSystemDefault())
        
        CodigosRecuperacion.insert {
            it[CodigosRecuperacion.email] = email
            it[CodigosRecuperacion.codigo] = code
            it[CodigosRecuperacion.expiracion] = exp
        }.insertedCount > 0
    }

    suspend fun validateRecoveryCode(email: String, code: String): Boolean = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        CodigosRecuperacion.selectAll().where { 
            (CodigosRecuperacion.email eq email) and 
            (CodigosRecuperacion.codigo eq code) and 
            (CodigosRecuperacion.expiracion greater now)
        }.count() > 0L
    }

    suspend fun resetPassword(email: String, newPass: String): Boolean = dbQuery {
        val updated = Usuarios.update({ Usuarios.email eq email }) {
            it[password] = newPass
        }
        if (updated > 0) {
            CodigosRecuperacion.deleteWhere { CodigosRecuperacion.email eq email }
            true
        } else false
    }

    // --- ADMIN ---

    suspend fun getAllUsersByRol(rol: String): List<User> = dbQuery {
        val targetRoles = when (rol.lowercase()) {
            "estudiante", "alumno" -> listOf("estudiante", "alumno")
            else -> listOf(rol.lowercase())
        }
        Usuarios.selectAll().where { Usuarios.rol.lowerCase() inList targetRoles }
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
        password = row[Usuarios.password],
        rol = row[Usuarios.rol],
        numeroUnidad = row[Usuarios.numeroUnidad],
        edad = row[Usuarios.edad],
        telefono = row[Usuarios.telefono],
        horario = row[Usuarios.horario]
    )
}

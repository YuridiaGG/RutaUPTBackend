package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.User
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

class AuthRepository {
    suspend fun findUserByEmail(email: String): User? = dbQuery {
        Usuarios.selectAll().where { Usuarios.email.lowerCase() eq email.trim().lowercase() }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun registerUser(user: User): Boolean = dbQuery {
        try {
            Usuarios.insert {
                it[Usuarios.nombre] = user.nombre
                it[Usuarios.apellidos] = user.apellidos
                it[Usuarios.email] = user.email.trim().lowercase()
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
            it[Usuarios.email] = user.email.trim().lowercase()
            if (user.password != null) it[Usuarios.password] = user.password
            it[Usuarios.rol] = user.rol.lowercase()
            it[Usuarios.edad] = user.edad
            it[Usuarios.telefono] = user.telefono
            it[Usuarios.numeroUnidad] = user.numeroUnidad
            it[Usuarios.horario] = user.horario
        } > 0
    }

    suspend fun getUserPassword(email: String): String? = dbQuery {
        Usuarios.selectAll().where { Usuarios.email.lowerCase() eq email.trim().lowercase() }
            .map { it[Usuarios.password] }
            .singleOrNull()
    }

    // --- SISTEMA DE RECUPERACIÓN ---

    suspend fun saveRecoveryCode(email: String, recoveryCode: String): Boolean = dbQuery {
        val cleanEmail = email.trim().lowercase()
        val expiracionMilis = Clock.System.now().plus(10, DateTimeUnit.MINUTE).toEpochMilliseconds()
        RecoveryTokens.insert {
            it[RecoveryTokens.email] = cleanEmail
            it[RecoveryTokens.code] = recoveryCode.trim()
            it[RecoveryTokens.expiry] = expiracionMilis
        }.insertedCount > 0
    }

    suspend fun validateRecoveryCode(email: String, recoveryCode: String): Boolean = dbQuery {
        val cleanEmail = email.trim().lowercase()
        val cleanCode = recoveryCode.trim()
        val now = Clock.System.now().toEpochMilliseconds()
        val match = RecoveryTokens.selectAll().where {
            (RecoveryTokens.email.lowerCase() eq cleanEmail) and
            (RecoveryTokens.code eq cleanCode) and
            (RecoveryTokens.expiry greater now)
        }.count() > 0
        match
    }

    suspend fun resetPassword(email: String, newPass: String): Boolean = dbQuery {
        val cleanEmail = email.trim().lowercase()
        val updated = Usuarios.update({ Usuarios.email.lowerCase() eq cleanEmail }) {
            it[password] = newPass
        }
        if (updated > 0) {
            RecoveryTokens.deleteWhere { RecoveryTokens.email.lowerCase() eq cleanEmail }
            true
        } else false
    }

    // --- ADMIN: Búsqueda de usuarios por rol (corregido para que no desaparezcan) ---
    suspend fun getAllUsersByRol(rol: String): List<User> = dbQuery {
        val r = rol.lowercase().trim()
        // Buscamos coincidencia exacta o que empiece por el nombre (ej: "chofer" encuentra "choferes")
        val searchPattern = when {
            r.startsWith("estud") -> "estud%"
            r.startsWith("alum") -> "alum%"
            r.startsWith("chof") -> "chof%"
            r.startsWith("cond") -> "cond%"
            r.endsWith("s") -> r.dropLast(1) + "%"
            else -> "$r%"
        }
        
        Usuarios.selectAll().where { Usuarios.rol.lowerCase() like searchPattern }
            .map { rowToUser(it) }
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

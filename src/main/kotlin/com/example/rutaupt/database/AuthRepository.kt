package com.example.rutaupt.database

import com.example.rutaupt.database.DatabaseFactory.dbQuery
import com.example.rutaupt.model.User
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater

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
        
        // Guardamos el código nuevo. NO borramos los anteriores.
        // Esto permite que si te llegan 3 correos, los 3 códigos funcionen.
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

        println("=== VALIDANDO CÓDIGO (Multi-Intento) ===")
        println("Email: '$cleanEmail' | Código ingresado: '$cleanCode'")

        // Buscamos si existe ALGÚN registro para este email con este código que no haya expirado
        val match = RecoveryTokens.selectAll().where {
            (RecoveryTokens.email.lowerCase() eq cleanEmail) and
            (RecoveryTokens.code eq cleanCode) and
            (RecoveryTokens.expiry greater now)
        }.count() > 0

        if (match) {
            println("¡ÉXITO! Código válido encontrado.")
            true
        } else {
            val activos = RecoveryTokens.selectAll()
                .where { (RecoveryTokens.email.lowerCase() eq cleanEmail) and (RecoveryTokens.expiry greater now) }
                .map { it[RecoveryTokens.code] }
            println("FALLO: El código '$cleanCode' no coincide. Códigos activos en DB: $activos")
            false
        }
    }

    suspend fun resetPassword(email: String, newPass: String): Boolean = dbQuery {
        val cleanEmail = email.trim().lowercase()
        val updated = Usuarios.update({ Usuarios.email.lowerCase() eq cleanEmail }) {
            it[password] = newPass
        }
        if (updated > 0) {
            // Al cambiar la contraseña, limpiamos todos los tokens
            RecoveryTokens.deleteWhere { RecoveryTokens.email.lowerCase() eq cleanEmail }
            true
        } else false
    }

    suspend fun getAllUsersByRol(rol: String): List<User> = dbQuery {
        val targetRoles = when (rol.lowercase()) {
            "estudiante", "alumno" -> listOf("estudiante", "alumno")
            else -> listOf(rol.lowercase())
        }
        Usuarios.selectAll().where { Usuarios.rol.lowerCase() inList targetRoles }
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

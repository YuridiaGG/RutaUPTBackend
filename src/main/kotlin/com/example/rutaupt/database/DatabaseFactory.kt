package com.example.rutaupt.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var dbInstance: Database? = null

    fun init() {
        // Railway inyecta estas variables automáticamente si el servicio MySQL está vinculado
        val host = System.getenv("MYSQLHOST") ?: "mysql.railway.internal"
        val port = System.getenv("MYSQLPORT") ?: "3306"
        val dbName = System.getenv("MYSQLDATABASE") ?: "railway"
        val user = System.getenv("MYSQLUSER") ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: "xBovtCQtJMzdcfPcLFsHcMZCHLrfCifY"

        // URL optimizada para la red interna de Railway
        val jdbcUrl = "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        
        try {
            val config = HikariConfig().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = password
                maximumPoolSize = 5
                connectionTimeout = 30000
                // Ayuda a detectar fugas de conexiones en el servidor
                leakDetectionThreshold = 2000
            }

            dbInstance = Database.connect(HikariDataSource(config))

            transaction(dbInstance) {
                SchemaUtils.create(Usuarios, Rutas, Paradas, Horarios, Reportes, UbicacionesTiempoReal)
                seedUser("admin@upt.com", "Admin", "Admin", "admin")
            }
            logger.info("¡CONEXIÓN EXITOSA! Servidor conectado a la base de datos en Railway.")
        } catch (e: Exception) {
            logger.error("FALLO CRÍTICO DE BASE DE DATOS: ${e.message}")
        }
    }

    private fun seedUser(email: String, nombre: String, apellidos: String, rol: String, unidad: String? = null) {
        if (Usuarios.selectAll().where { Usuarios.email eq email }.count() == 0L) {
            Usuarios.insert {
                it[Usuarios.nombre] = nombre
                it[Usuarios.apellidos] = apellidos
                it[Usuarios.email] = email
                it[Usuarios.password] = "123"
                it[Usuarios.rol] = rol
                it[Usuarios.numeroUnidad] = unidad
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db = dbInstance) { block() }
}

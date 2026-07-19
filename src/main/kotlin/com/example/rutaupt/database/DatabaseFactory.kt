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
        val host = System.getenv("MYSQLHOST") ?: "reseau.proxy.rlwy.net"
        val port = System.getenv("MYSQLPORT") ?: "52875"
        val dbName = System.getenv("MYSQLDATABASE") ?: "railway"
        val user = System.getenv("MYSQLUSER") ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: "xBovtCQtJMzdcfPcLFsHcMZCHLrfCifY"

        val jdbcUrl = "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        
        try {
            val config = HikariConfig().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = password
                maximumPoolSize = 3
                connectionTimeout = 10000
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

            dbInstance = Database.connect(HikariDataSource(config))

            transaction(dbInstance) {
                // DROP ELIMINADO DEFINITIVAMENTE para que los datos sean persistentes
                SchemaUtils.create(Usuarios, Rutas, Paradas, Horarios, Reportes, UbicacionesTiempoReal)
                
                // Asegura que se cree la columna 'horario' si no existe
                SchemaUtils.createMissingTablesAndColumns(Usuarios)
                
                seedUser("admin@upt.com", "Admin", "Admin", "admin")
            }
            logger.info("¡CONEXIÓN EXITOSA! Esquema de base de datos listo y persistente.")
        } catch (e: Exception) {
            logger.error("FALLO DE CONEXIÓN: ${e.message}")
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

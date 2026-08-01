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
        // Priorizamos variables de entorno para producción (Railway)
        // Pero usamos localhost como respaldo para desarrollo local, igual que tenías en la raíz
        val host = System.getenv("MYSQLHOST") ?: "localhost"
        val port = System.getenv("MYSQLPORT") ?: "3306"
        val dbName = System.getenv("MYSQLDATABASE") ?: "railway"
        val user = System.getenv("MYSQLUSER") ?: "root"
        val password = System.getenv("MYSQLPASSWORD") ?: ""

        val jdbcUrl = "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"
        
        try {
            val config = HikariConfig().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = password
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 30000
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

            dbInstance = Database.connect(HikariDataSource(config))

            transaction(dbInstance) {
                // Creamos las tablas si no existen
                SchemaUtils.create(Usuarios, Rutas, Paradas, Horarios, Reportes, UbicacionesTiempoReal)
                
                // Sincronizamos columnas nuevas sin borrar datos
                SchemaUtils.createMissingTablesAndColumns(Usuarios, Rutas, Paradas, Horarios, Reportes, UbicacionesTiempoReal)
                
                seedUser("admin@upt.com", "Admin", "Admin", "admin")
            }
            logger.info("Base de datos conectada correctamente (Host: $host)")
        } catch (e: Exception) {
            logger.error("ERROR DE CONEXIÓN A BASE DE DATOS: ${e.message}")
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

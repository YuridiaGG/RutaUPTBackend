package com.example.rutaupt.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Usuarios : Table("usuarios") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 100)
    val apellidos = varchar("apellidos", 100)
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 255)
    val rol = varchar("rol", 20)
    val edad = varchar("edad", 5).nullable()
    val telefono = varchar("telefono", 20).nullable()
    val numeroUnidad = varchar("numero_unidad", 20).nullable()
    val horario = varchar("horario", 100).nullable()
    override val primaryKey = PrimaryKey(id)
}

object CodigosRecuperacion : Table("codigos_recuperacion") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 100)
    val codigo = varchar("codigo", 6)
    val expiracion = datetime("expiracion")
    override val primaryKey = PrimaryKey(id)
}

object Rutas : Table("rutas") {
    val idRuta = integer("id_ruta").autoIncrement()
    val nombreRuta = varchar("nombre_ruta", 100)
    val color = varchar("color", 7)
    val activa = bool("activa").default(true)
    override val primaryKey = PrimaryKey(idRuta)
}

object Paradas : Table("paradas") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 100)
    val ubicacion = text("ubicacion").nullable()
    val latitud = decimal("latitud", 10, 8).nullable()
    val longitud = decimal("longitud", 11, 8).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Reportes : Table("reportes") {
    val id = long("id").autoIncrement()
    val unidad = varchar("unidad", 20)
    val mensaje = text("mensaje")
    val fechaHora = varchar("fecha_hora", 50)
    val tipo = varchar("tipo", 50)
    val imagen = text("imagen").nullable()
    val estado = varchar("estado", 50).nullable()
    val validacionAdmin = text("validacion_admin").nullable()
    override val primaryKey = PrimaryKey(id)
}

object UbicacionesTiempoReal : Table("ubicaciones_tiempo_real") {
    val numeroUnidad = varchar("numero_unidad", 20)
    val latitud = decimal("latitud", 10, 8)
    val longitud = decimal("longitud", 11, 8)
    val ultimaActualizacion = varchar("ultima_actualizacion", 30)
    override val primaryKey = PrimaryKey(numeroUnidad)
}

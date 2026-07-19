package com.example.rutaupt.database

import org.jetbrains.exposed.sql.Table

object Usuarios : Table("usuarios") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 100)
    val apellidos = varchar("apellidos", 100)
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 255)
    val rol = varchar("rol", 20) // 'admin', 'estudiante', 'chofer'
    val edad = varchar("edad", 5).nullable()
    val telefono = varchar("telefono", 20).nullable()
    val numeroUnidad = varchar("numero_unidad", 20).nullable()
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
    override val primaryKey = PrimaryKey(id)
}

object Horarios : Table("horarios") {
    val idHorario = integer("id_horario").autoIncrement()
    val idRuta = integer("id_ruta").references(Rutas.idRuta)
    val idUsuarioChofer = integer("id_usuario_chofer").references(Usuarios.id)
    val horaSalida = varchar("hora_salida", 8) 
    val dias = varchar("dias", 50)
    override val primaryKey = PrimaryKey(idHorario)
}

object Reportes : Table("reportes") {
    val id = long("id").autoIncrement()
    val unidad = varchar("unidad", 20)
    val mensaje = text("mensaje")
    val fechaHora = varchar("fecha_hora", 30) 
    val tipo = varchar("tipo", 20) // 'ALERTA' o 'INFORMACION'
    val imagen = text("imagen").nullable() 
    val estado = varchar("estado", 50).nullable()
    val validacionAdmin = varchar("validacion_admin", 20).nullable()
    override val primaryKey = PrimaryKey(id)
}

object UbicacionesTiempoReal : Table("ubicaciones_tiempo_real") {
    val numeroUnidad = varchar("numero_unidad", 20)
    val latitud = decimal("latitud", 10, 8)
    val longitud = decimal("longitud", 11, 8)
    val ultimaActualizacion = varchar("ultima_actualizacion", 30)
    override val primaryKey = PrimaryKey(numeroUnidad)
}

package com.example.rutaupt

import com.example.rutaupt.database.*
import com.example.rutaupt.model.*
import com.example.rutaupt.api.EmailService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class ParadaRequest(
    val nombre: String,
    val ubicacion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class ReporteStatusRequest(val estado: String, val validacionAdmin: String? = null)

// Clases de petición más flexibles
@Serializable
data class VerifyCodeRequest(
    val email: String? = null,
    val mail: String? = null,
    val code: String? = null,
    val codigo: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val email: String? = null,
    val mail: String? = null,
    val code: String? = null,
    val codigo: String? = null,
    val newPassword: String? = null,
    val pass: String? = null
)

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080,
        host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        anyHost()
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("ERROR NO CONTROLADO: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to cause.message))
        }
    }

    DatabaseFactory.init()
    val authRepository = AuthRepository()
    val paradasRepository = ParadasRepository()
    val reportesRepository = ReportesRepository()
    val rutasRepository = RutasRepository()

    routing {
        get("/") { call.respondText("Servidor RutaUPT Online") }

        // --- AUTH ---
        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            val user = authRepository.findUserByEmail(request.email)
            val dbPass = authRepository.getUserPassword(request.email)
            if (user != null && dbPass == request.pass) {
                call.respond(LoginResponse(true, "OK", user))
            } else {
                call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Credenciales incorrectas"))
            }
        }

        post("/api/auth/register") {
            val user = call.receive<User>()
            if (authRepository.registerUser(user)) {
                EmailService.sendWelcomeEmail(user.nombre, user.email, user.rol)
                call.respond(HttpStatusCode.Created, RegisterResponse(true, "OK"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error DB"))
            }
        }

        // 1. Enviar código
        post("/api/auth/recover") {
            try {
                val request = call.receive<RecoveryRequest>()
                val user = authRepository.findUserByEmail(request.email)
                if (user != null) {
                    val code = Random.nextInt(100000, 999999).toString()
                    if (authRepository.saveRecoveryCode(user.email, code)) {
                        val sent = EmailService.sendVerificationCode(user.nombre, user.email, code)
                        if (sent) call.respond(RegisterResponse(true, "Código enviado correctamente"))
                        else call.respond(HttpStatusCode.OK, RegisterResponse(false, "Fallo al enviar correo"))
                    } else {
                        call.respond(HttpStatusCode.OK, RegisterResponse(false, "Error al generar código"))
                    }
                } else {
                    call.respond(HttpStatusCode.OK, RegisterResponse(false, "Email no registrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "Error: ${e.message}"))
            }
        }

        // 2. Validar código (Corregido a verify-code para coincidir con la App)
        post("/api/auth/verify-code") {
            try {
                val request = call.receive<VerifyCodeRequest>()
                val email = request.email ?: request.mail ?: ""
                val code = request.code ?: request.codigo ?: ""
                
                if (authRepository.validateRecoveryCode(email, code)) {
                    call.respond(RegisterResponse(true, "Código válido"))
                } else {
                    call.respond(HttpStatusCode.OK, RegisterResponse(false, "Código incorrecto o expirado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "Error: ${e.message}"))
            }
        }

        // 3. Cambiar contraseña
        post("/api/auth/reset-password") {
            try {
                val request = call.receive<ResetPasswordRequest>()
                val email = request.email ?: request.mail ?: ""
                val code = request.code ?: request.codigo ?: ""
                val newPass = request.newPassword ?: request.pass ?: ""
                
                if (authRepository.validateRecoveryCode(email, code)) {
                    if (authRepository.resetPassword(email, newPass)) {
                        call.respond(RegisterResponse(true, "Contraseña actualizada"))
                    } else {
                        call.respond(HttpStatusCode.OK, RegisterResponse(false, "Error al actualizar"))
                    }
                } else {
                    call.respond(HttpStatusCode.OK, RegisterResponse(false, "Código inválido o expirado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "Error: ${e.message}"))
            }
        }

        post("/api/auth/update") {
            try {
                val user = call.receive<User>()
                if (authRepository.updateUser(user)) {
                    call.respond(HttpStatusCode.OK, RegisterResponse(true, "Usuario actualizado"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "No se encontró el ID"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error: ${e.message}"))
            }
        }

        // --- RUTAS / PARADAS / REPORTES ---
        get("/api/rutas") { call.respond(rutasRepository.getAllRutas()) }
        get("/api/paradas") { call.respond(paradasRepository.getAllParadas()) }
        post("/api/paradas") {
            try {
                val req = call.receive<ParadaRequest>()
                val id = paradasRepository.addParada(Parada(nombre = req.nombre, ubicacion = req.ubicacion, latitud = req.latitud, longitud = req.longitud))
                if (id != null) call.respond(HttpStatusCode.Created, mapOf("success" to true, "id" to id))
                else call.respond(HttpStatusCode.InternalServerError)
            } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, e.message ?: "") }
        }
        
        // --- REPORTES ---
        route("/api/reportes") {
            get { call.respond(reportesRepository.getAllReportes()) }
            post {
                try {
                    val rep = call.receive<ReporteUnidad>()
                    val id = reportesRepository.addReporte(rep)
                    if (id != null) call.respond(HttpStatusCode.Created, mapOf("success" to true, "id" to id))
                    else call.respond(HttpStatusCode.InternalServerError)
                } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest) }
            }
            put("/{id}/validar") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val req = call.receive<ReporteStatusRequest>()
                if (reportesRepository.updateReporteEstado(id, req.estado, req.validacionAdmin)) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.NotFound)
            }
        }

        // --- ADMIN ---
        get("/api/admin/stats") {
            val est = authRepository.getAllUsersByRol("estudiante").size
            val cho = authRepository.getAllUsersByRol("chofer").size
            val rut = rutasRepository.getRutasCount()
            call.respond(mapOf("estudiantes" to est, "choferes" to cho, "rutas" to rut))
        }
        get("/api/admin/users/{rol}") {
            val rol = call.parameters["rol"] ?: "chofer"
            call.respond(authRepository.getAllUsersByRol(rol))
        }
    }
}

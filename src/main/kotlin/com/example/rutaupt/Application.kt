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
import kotlinx.coroutines.launch

@Serializable
data class ParadaRequest(
    val nombre: String,
    val ubicacion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class ReporteStatusRequest(val estado: String, val validacionAdmin: String? = null)

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
            logger.error("ERROR CRÍTICO: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Error interno del servidor"))
        }
    }

    DatabaseFactory.init()
    val authRepository = AuthRepository()
    val paradasRepository = ParadasRepository()
    val reportesRepository = ReportesRepository()
    val rutasRepository = RutasRepository()

    routing {
        get("/") { call.respondText("Servidor RutaUPT Online") }

        // --- LOGIN ---
        post("/api/auth/login") {
            try {
                val request = call.receive<LoginRequest>()
                val email = request.email ?: request.mail ?: ""
                val password = request.pass ?: request.password ?: ""

                val user = authRepository.findUserByEmail(email)
                val dbPass = authRepository.getUserPassword(email)

                if (user != null && dbPass == password) {
                    call.respond(LoginResponse(true, "OK", user))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Correo o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, LoginResponse(false, "Error en los datos enviados"))
            }
        }

        // --- VERIFICAR CÓDIGO (Corregido para devolver el usuario) ---
        post("/api/auth/verify-code") {
            try {
                val request = call.receive<VerifyCodeRequest>()
                val email = request.email ?: request.mail ?: ""
                val code = request.code ?: request.codigo ?: ""
                
                if (authRepository.validateRecoveryCode(email, code)) {
                    val user = authRepository.findUserByEmail(email)
                    // Devolvemos LoginResponse en lugar de RegisterResponse para incluir al usuario
                    call.respond(LoginResponse(true, "Código válido", user))
                } else {
                    call.respond(HttpStatusCode.OK, LoginResponse(false, "Código incorrecto o expirado"))
                }
            } catch (e: Exception) {
                logger.error("Error en verify-code: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, LoginResponse(false, "Error al procesar la verificación"))
            }
        }

        // --- RECUPERAR (ENVIAR CÓDIGO) ---
        post("/api/auth/recover") {
            try {
                val request = call.receive<RecoveryRequest>()
                val email = request.email ?: request.mail ?: ""
                val user = authRepository.findUserByEmail(email)
                if (user != null) {
                    val code = Random.nextInt(100000, 999999).toString()
                    if (authRepository.saveRecoveryCode(email, code)) {
                        launch { EmailService.sendVerificationCode(user.nombre, email, code) }
                        call.respond(RegisterResponse(true, "Código enviado"))
                    } else {
                        call.respond(RegisterResponse(false, "Error al generar código"))
                    }
                } else {
                    call.respond(RegisterResponse(false, "El correo no está registrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "Error en la solicitud"))
            }
        }

        // --- RESTABLECER CONTRASEÑA ---
        post("/api/auth/reset-password") {
            try {
                val request = call.receive<ResetPasswordRequest>()
                val email = request.email ?: request.mail ?: ""
                val code = request.code ?: request.codigo ?: ""
                val newPass = request.newPassword ?: request.pass ?: ""
                
                if (authRepository.validateRecoveryCode(email, code)) {
                    if (authRepository.resetPassword(email, newPass)) {
                        val user = authRepository.findUserByEmail(email)
                        call.respond(LoginResponse(true, "Contraseña actualizada", user))
                    } else {
                        call.respond(LoginResponse(false, "No se pudo actualizar"))
                    }
                } else {
                    call.respond(LoginResponse(false, "Código inválido"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, LoginResponse(false, "Error al restablecer"))
            }
        }

        // --- REGISTRO ---
        post("/api/auth/register") {
            try {
                val user = call.receive<User>()
                if (authRepository.registerUser(user)) {
                    launch { EmailService.sendWelcomeEmail(user.nombre, user.email, user.rol) }
                    call.respond(HttpStatusCode.Created, RegisterResponse(true, "Registro exitoso"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error en DB"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, RegisterResponse(false, "Datos inválidos"))
            }
        }

        get("/api/rutas") { call.respond(rutasRepository.getAllRutas()) }
        get("/api/paradas") { call.respond(paradasRepository.getAllParadas()) }
        
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
        }
    }
}

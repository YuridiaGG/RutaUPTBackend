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

@Serializable
data class ParadaRequest(val nombre: String)

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
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("ERROR NO CONTROLADO: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error en el servidor: ${cause.message}"))
        }
    }

    DatabaseFactory.init()
    val authRepository = AuthRepository()
    val paradasRepository = ParadasRepository()
    val reportesRepository = ReportesRepository()

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
            if (authRepository.registerUser(user)) call.respond(HttpStatusCode.Created, RegisterResponse(true, "OK"))
            else call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error DB"))
        }

        post("/api/auth/recover") {
            val request = call.receive<RecoveryRequest>()
            val user = authRepository.findUserByEmail(request.email)
            if (user != null) {
                val pass = user.password ?: ""
                val sent = EmailService.sendPasswordRecoveryEmail(user.nombre, user.email, pass)
                if (sent) call.respond(RegisterResponse(true, "Correo enviado correctamente"))
                else call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error SMTP"))
            } else {
                call.respond(HttpStatusCode.NotFound, RegisterResponse(false, "Email no registrado"))
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

        // --- PARADAS ---
        get("/api/paradas") {
            call.respond(paradasRepository.getAllParadas())
        }

        post("/api/paradas") {
            val request = call.receive<ParadaRequest>()
            if (paradasRepository.addParada(request.nombre)) call.respond(HttpStatusCode.Created, mapOf("success" to true))
            else call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false))
        }

        delete("/api/paradas/{nombre}") {
            val nombre = call.parameters["nombre"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (paradasRepository.deleteParadaByName(nombre)) call.respond(HttpStatusCode.OK, mapOf("success" to true))
            else call.respond(HttpStatusCode.NotFound, mapOf("success" to false))
        }

        // --- REPORTES ---
        get("/api/reportes") {
            call.respond(reportesRepository.getAllReportes())
        }

        post("/api/reportes") {
            try {
                val reporte = call.receive<ReporteUnidad>()
                if (reportesRepository.addReporte(reporte)) {
                    call.respond(HttpStatusCode.Created, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Error al guardar en DB"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to e.message))
            }
        }

        // --- ADMIN ---
        get("/api/admin/stats") {
            try {
                val est = authRepository.getAllUsersByRol("estudiante").size
                val cho = authRepository.getAllUsersByRol("chofer").size
                call.respond(mapOf("estudiantes" to est, "choferes" to cho, "rutas" to 0))
            } catch (e: Exception) {
                call.respond(mapOf("estudiantes" to 0, "choferes" to 0, "rutas" to 0))
            }
        }

        get("/api/admin/users/{rol}") {
            val rol = call.parameters["rol"] ?: "chofer"
            call.respond(authRepository.getAllUsersByRol(rol))
        }

        delete("/api/admin/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (authRepository.deleteUser(id)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}

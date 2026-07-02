package com.example.rutaupt

import com.example.rutaupt.database.AuthRepository
import com.example.rutaupt.database.DatabaseFactory
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

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080,
        host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")

    // CONFIGURACIÓN DE CORS: Permite que tu Frontend se conecte a este Backend
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        anyHost() // Permite cualquier origen (Frontend) durante desarrollo
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

    routing {
        get("/") { call.respondText("Servidor RutaUPT Online") }

        // --- RUTAS DE AUTH ---
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
            val pass = authRepository.getUserPassword(request.email)
            if (pass != null) {
                val sent = EmailService.sendPasswordRecoveryEmail(request.email, pass)
                if (sent) call.respond(RegisterResponse(true, "Correo enviado"))
                else call.respond(HttpStatusCode.InternalServerError, RegisterResponse(false, "Error SMTP: Revisa variables en Railway"))
            } else {
                call.respond(HttpStatusCode.NotFound, RegisterResponse(false, "Email no registrado"))
            }
        }

        // --- RUTAS DE ADMIN (Corrigiendo el 404) ---
        get("/api/admin/stats") {
            try {
                val estudiantes = authRepository.getAllUsersByRol("estudiante").size
                val choferes = authRepository.getAllUsersByRol("chofer").size
                call.respond(mapOf(
                    "estudiantes" to estudiantes,
                    "choferes" to choferes,
                    "rutas" to 0
                ))
            } catch (e: Exception) {
                call.respond(mapOf("estudiantes" to 0, "choferes" to 0, "rutas" to 0))
            }
        }

        get("/api/admin/users/{rol}") {
            val rol = call.parameters["rol"] ?: "chofer"
            val users = authRepository.getAllUsersByRol(rol)
            call.respond(users)
        }

        delete("/api/admin/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (authRepository.deleteUser(id)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}

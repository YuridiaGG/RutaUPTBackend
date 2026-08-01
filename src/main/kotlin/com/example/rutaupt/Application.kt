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
data class ParadaRequest(val nombre: String, val ubicacion: String? = null)

@Serializable
data class ReporteStatusRequest(val estado: String, val validacionAdmin: String? = null)

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

        // --- REPORTES (BLOQUE ORGANIZADO) ---
        route("/api/reportes") {
            get {
                call.respond(reportesRepository.getAllReportes())
            }

            post {
                try {
                    val reporte = call.receive<ReporteUnidad>()
                    val newId = reportesRepository.addReporte(reporte)
                    if (newId != null) {
                        call.respond(HttpStatusCode.Created, mapOf("success" to true, "id" to newId))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to e.message))
                }
            }

            route("/{id}") {
                // Este es el endpoint que tu App llama para aceptar/denegar
                put("/validar") {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, "ID inválido")
                        return@put
                    }
                    val request = call.receive<ReporteStatusRequest>()
                    if (reportesRepository.updateReporteEstado(id, request.estado, request.validacionAdmin)) {
                        call.respond(HttpStatusCode.OK, mapOf("success" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "No se pudo actualizar el reporte $id"))
                    }
                }

                put {
                    val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<ReporteStatusRequest>()
                    if (reportesRepository.updateReporteEstado(id, request.estado, request.validacionAdmin)) {
                        call.respond(HttpStatusCode.OK, mapOf("success" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                delete {
                    val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (reportesRepository.deleteReporte(id)) {
                        call.respond(HttpStatusCode.OK, mapOf("success" to true))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("success" to false))
                    }
                }
            }
        }

        // --- OTROS ---
        get("/api/paradas") { call.respond(paradasRepository.getAllParadas()) }
        delete("/api/paradas/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (paradasRepository.deleteParadaById(id)) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.NotFound)
        }
        
        get("/api/admin/stats") {
            val est = authRepository.getAllUsersByRol("estudiante").size
            val cho = authRepository.getAllUsersByRol("chofer").size
            val rut = rutasRepository.getRutasCount()
            call.respond(mapOf("estudiantes" to est, "choferes" to cho, "rutas" to rut))
        }
    }
}

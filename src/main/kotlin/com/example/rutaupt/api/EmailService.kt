package com.example.rutaupt.api

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BrevoSender(val name: String, val email: String)

@Serializable
data class BrevoTo(val email: String, val name: String? = null)

@Serializable
data class BrevoEmailRequest(
    val sender: BrevoSender,
    val to: List<BrevoTo>,
    val subject: String,
    val htmlContent: String
)

object EmailService {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private suspend fun sendEmail(name: String, to: String, subject: String, html: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = System.getenv("BREVO_API_KEY")?.trim() ?: ""
        val senderEmail = System.getenv("SENDER_EMAIL")?.trim() ?: ""

        if (apiKey.isEmpty() || senderEmail.isEmpty()) {
            println("ERROR: Faltan variables BREVO_API_KEY o SENDER_EMAIL.")
            return@withContext false
        }

        try {
            val response: HttpResponse = client.post("https://api.brevo.com/v3/smtp/email") {
                header("api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(BrevoEmailRequest(
                    sender = BrevoSender("RutaUPT Soporte", senderEmail),
                    to = listOf(BrevoTo(to, name)),
                    subject = subject,
                    htmlContent = html
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Error enviando email: ${e.message}")
            false
        }
    }

    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean {
        val html = """
            <div style="font-family: sans-serif; padding: 20px;">
                <h2>Recuperación de Contraseña</h2>
                <p>Hola $name, tu contraseña es: <strong>$password</strong></p>
                <p>Cámbiala pronto desde tu perfil.</p>
            </div>
        """.trimIndent()
        return sendEmail(name, to, "Recuperación de contraseña - RutaUPT", html)
    }

    suspend fun sendWelcomeEmail(name: String, to: String, rol: String): Boolean {
        val html = """
            <div style="font-family: sans-serif; padding: 20px;">
                <h2>¡Bienvenido a RutaUPT!</h2>
                <p>Hola $name, tu cuenta como <strong>$rol</strong> ha sido creada.</p>
            </div>
        """.trimIndent()
        return sendEmail(name, to, "Bienvenido a RutaUPT", html)
    }
}

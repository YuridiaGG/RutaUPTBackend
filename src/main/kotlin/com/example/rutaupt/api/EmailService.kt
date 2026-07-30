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

    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = System.getenv("BREVO_API_KEY")?.trim() ?: ""
        val senderEmail = System.getenv("SENDER_EMAIL")?.trim() ?: ""

        if (apiKey.isEmpty() || senderEmail.isEmpty()) {
            println("ERROR: Faltan variables BREVO_API_KEY o SENDER_EMAIL en Railway.")
            return@withContext false
        }

        try {
            println("Iniciando envío universal vía Brevo para: $to")
            
            val response: HttpResponse = client.post("https://api.brevo.com/v3/smtp/email") {
                header("api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(BrevoEmailRequest(
                    sender = BrevoSender("RutaUPT Soporte", senderEmail),
                    to = listOf(BrevoTo(to, name)),
                    subject = "Recuperación de credenciales – RutaUPT",
                    htmlContent = """
                        <div style="font-family: sans-serif; padding: 20px; color: #333; line-height: 1.6;">
                            <h2 style="color: #007bff;">Hola $name,</h2>
                            <p>Has solicitado tus credenciales de acceso para <strong>RutaUPT</strong>:</p>
                            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 8px; border: 1px solid #dee2e6; margin: 20px 0;">
                                <p style="margin: 5px 0;"><strong>Email:</strong> $to</p>
                                <p style="margin: 5px 0;"><strong>Contraseña:</strong> $password</p>
                            </div>
                            <p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>
                            <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="font-size: 12px; color: #666;">Saludos,<br>Equipo RutaUPT</p>
                        </div>
                    """.trimIndent()
                ))
            }

            if (response.status.isSuccess()) {
                println("¡ÉXITO: Correo universal enviado correctamente vía Brevo!")
                true
            } else {
                val errorBody = response.bodyAsText()
                println("Error de API Brevo (${response.status}): $errorBody")
                false
            }
        } catch (e: Exception) {
            println("FALLO CRÍTICO EN API BREVO: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

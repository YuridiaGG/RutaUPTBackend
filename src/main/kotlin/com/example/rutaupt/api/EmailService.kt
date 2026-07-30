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
data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String
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
        val apiKey = System.getenv("RESEND_API_KEY")?.trim() ?: ""
        // Por defecto usamos el email de prueba de Resend si no hay uno configurado
        val fromEmail = System.getenv("FROM_EMAIL")?.trim() ?: "onboarding@resend.dev"

        if (apiKey.isEmpty()) {
            println("ERROR: Falta la variable RESEND_API_KEY en Railway.")
            return@withContext false
        }

        try {
            println("Iniciando envío vía API de Resend para: $to")
            
            val response: HttpResponse = client.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ResendEmailRequest(
                    from = fromEmail,
                    to = listOf(to),
                    subject = "Recuperación de credenciales – RutaUPT",
                    html = """
                        <div style="font-family: sans-serif; color: #333; padding: 20px; line-height: 1.5;">
                            <h2 style="color: #007bff;">Hola $name,</h2>
                            <p>Has solicitado tus credenciales de acceso para <strong>RutaUPT</strong>:</p>
                            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; border: 1px solid #dee2e6; margin: 20px 0;">
                                <p style="margin: 5px 0;"><strong>Email:</strong> $to</p>
                                <p style="margin: 5px 0;"><strong>Contraseña:</strong> $password</p>
                            </div>
                            <p>Si no solicitaste este correo, puedes ignorarlo con seguridad.</p>
                            <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="font-size: 12px; color: #666;">Saludos,<br>Equipo RutaUPT</p>
                        </div>
                    """.trimIndent()
                ))
            }

            if (response.status.isSuccess()) {
                println("¡ÉXITO: Correo enviado correctamente vía API de Resend!")
                true
            } else {
                val errorBody = response.bodyAsText()
                println("Error de API Resend (${response.status}): $errorBody")
                false
            }
        } catch (e: Exception) {
            println("FALLO CRÍTICO EN API RESEND: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

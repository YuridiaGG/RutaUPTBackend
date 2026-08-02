package com.example.rutaupt.api

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
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
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }

    private suspend fun sendEmail(name: String, to: String, subject: String, html: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = System.getenv("BREVO_API_KEY")?.trim() ?: ""
        val senderEmail = System.getenv("SENDER_EMAIL")?.trim() ?: ""

        if (apiKey.isEmpty() || senderEmail.isEmpty()) {
            println("ERROR: BREVO_API_KEY o SENDER_EMAIL no están configuradas en las variables de entorno.")
            return@withContext false
        }

        try {
            println("Iniciando envío de correo a $to...")
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
            
            if (response.status.isSuccess()) {
                println("¡Correo enviado exitosamente a $to!")
                true
            } else {
                val errorBody = response.bodyAsText()
                println("Error de Brevo (Status: ${response.status}): $errorBody")
                false
            }
        } catch (e: HttpRequestTimeoutException) {
            println("Error: Tiempo de espera excedido al contactar con Brevo.")
            false
        } catch (e: Exception) {
            println("Error inesperado enviando email a $to: ${e.message}")
            false
        }
    }

    suspend fun sendVerificationCode(name: String, to: String, code: String): Boolean {
        val html = """
            <div style="font-family: sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px; line-height: 1.6;">
                <h2 style="color: #004a99; text-align: center;">Recuperación de contraseña – RutaUPT</h2>
                <p>Estimado(a) usuario de RutaUPT:</p>
                <p>Hemos recibido una solicitud para recuperar el acceso a tu cuenta.</p>
                <p>Para continuar con el proceso, utiliza el siguiente código de verificación:</p>
                
                <div style="background-color: #f2f7ff; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; border: 1px solid #004a99;">
                    <span style="font-size: 14px; color: #666; display: block; margin-bottom: 5px;">Código de recuperación:</span>
                    <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #004a99;">$code</span>
                </div>

                <p>Este código es válido por <strong>10 minutos</strong> y solo puede utilizarse una vez.</p>
                <p>Si no realizaste esta solicitud, puedes ignorar este mensaje.</p>
                
                <p>Gracias por utilizar RutaUPT.</p>
                
                <div style="margin-top: 30px; border-top: 1px solid #eee; padding-top: 10px; font-size: 13px; color: #555;">
                    <p style="margin: 0;">Atentamente,</p>
                    <p style="margin: 0; font-weight: bold;">Equipo de Soporte RutaUPT</p>
                </div>
            </div>
        """.trimIndent()
        return sendEmail(name, to, "Recuperación de contraseña – RutaUPT", html)
    }

    suspend fun sendWelcomeEmail(name: String, to: String, rol: String): Boolean {
        val html = """
            <div style="font-family: sans-serif; padding: 20px;">
                <h2>¡Bienvenido a RutaUPT!</h2>
                <p>Hola $name, tu cuenta como <strong>$rol</strong> ha sido creada exitosamente.</p>
            </div>
        """.trimIndent()
        return sendEmail(name, to, "Bienvenido a RutaUPT", html)
    }
}

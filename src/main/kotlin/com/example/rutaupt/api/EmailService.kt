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
                    subject = "Recuperación de contraseña - RutaUPT",
                    htmlContent = """
                        <div style="font-family: sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px; line-height: 1.6;">
                            <h3 style="color: #004a99; border-bottom: 2px solid #004a99; padding-bottom: 10px;">RutaUPT - Soporte</h3>
                            <p>Estimado(a) <strong>$name</strong>:</p>
                            <p>Atendiendo tu solicitud de recuperación de contraseña, te proporcionamos la información de acceso a tu cuenta.</p>
                            
                            <div style="background-color: #f2f7ff; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; border: 1px dashed #004a99;">
                                <span style="font-size: 18px; color: #004a99;">Contraseña: <strong>$password</strong></span>
                            </div>

                            <p>Por tu seguridad, te recomendamos iniciar sesión lo antes posible y actualizar esta contraseña desde la sección <strong>"Perfil"</strong>, creando una nueva que solo tú conozcas.</p>
                            
                            <p style="font-size: 13px; color: #666;">Si no solicitaste esta recuperación, comunícate con el administrador del sistema para proteger tu cuenta.</p>
                            
                            <p>Gracias por utilizar RutaUPT.</p>
                            
                            <div style="margin-top: 30px; border-top: 1px solid #eee; padding-top: 10px; font-size: 12px; color: #888;">
                                <p style="margin: 0;">Atentamente,</p>
                                <p style="margin: 0; font-weight: bold;">Equipo de Soporte RutaUPT</p>
                                <p style="margin: 0;">Universidad Politécnica de Tulancingo</p>
                            </div>
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

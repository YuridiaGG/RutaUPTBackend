package com.example.rutaupt.api

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmailService {
    // Implementación en Dispatchers.IO para no bloquear el servidor
    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
        val smtpPort = System.getenv("SMTP_PORT") ?: "587"
        val smtpUser = System.getenv("SMTP_USER") ?: ""
        val smtpPass = System.getenv("SMTP_PASS") ?: ""

        if (smtpUser.isEmpty() || smtpPass.isEmpty()) {
            println("Error: Credenciales SMTP (USER/PASS) no configuradas en variables de entorno.")
            return@withContext false
        }

        val prop = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.ssl.trust", smtpHost)
        }

        val session = Session.getInstance(prop, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUser, smtpPass)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de credenciales – RutaUPT"
                setText("Hola $name, tus credenciales de acceso para RutaUPT son: Email: $to | Contraseña: $password")
            }
            Transport.send(message)
            true
        } catch (e: Exception) {
            println("Error al enviar email: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

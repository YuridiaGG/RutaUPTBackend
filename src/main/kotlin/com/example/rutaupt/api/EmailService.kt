package com.example.rutaupt.api

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmailService {
    // REQUISITO: La función de enviar correo debe ejecutarse en un Dispatchers.IO para no bloquear el servidor.
    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val smtpHost = System.getenv("SMTP_HOST")?.trim() ?: "smtp.gmail.com"
        val smtpPort = System.getenv("SMTP_PORT")?.trim() ?: "587"
        val smtpUser = System.getenv("SMTP_USER")?.trim() ?: ""
        val smtpPass = System.getenv("SMTP_PASS")?.trim() ?: ""

        if (smtpUser.isEmpty() || smtpPass.isEmpty()) {
            println("ERROR SMTP: Faltan variables de entorno SMTP_USER o SMTP_PASS.")
            return@withContext false
        }

        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2")
            put("mail.smtp.ssl.trust", smtpHost)
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtpUser, smtpPass)
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "RutaUPT"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de credenciales – RutaUPT"
                
                // REQUISITO: Mensaje exacto solicitado
                setText("Hola $name, tus credenciales de acceso para RutaUPT son: Email: $to | Contraseña: $password")
            }
            
            println("Conectando con SMTP para enviar correo a $to...")
            Transport.send(message)
            println("¡ÉXITO: Correo enviado correctamente!")
            true
        } catch (e: Exception) {
            println("FALLO CRÍTICO SMTP: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

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
        val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
        val smtpPort = System.getenv("SMTP_PORT") ?: "587"
        val smtpUser = System.getenv("SMTP_USER") ?: ""
        val smtpPass = System.getenv("SMTP_PASS") ?: "" // Contraseña de Aplicación

        if (smtpUser.isEmpty() || smtpPass.isEmpty()) {
            println("ERROR SMTP: Faltan las variables SMTP_USER o SMTP_PASS en Railway.")
            return@withContext false
        }

        val props = Properties().apply {
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.trust", smtpHost)
            // Timeouts para evitar que el servidor se quede colgado (demora mucho)
            put("mail.smtp.connectiontimeout", "10000") 
            put("mail.smtp.timeout", "10000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtpUser, smtpPass)
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "RutaUPT Soporte"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de credenciales – RutaUPT"
                // REQUISITO: Mensaje exacto solicitado
                setText("Hola $name, tus credenciales de acceso para RutaUPT son: Email: $to | Contraseña: $password")
            }
            
            println("Intentando enviar correo a $to...")
            Transport.send(message)
            println("¡Correo enviado exitosamente!")
            true
        } catch (e: Exception) {
            println("FALLO CRÍTICO AL ENVIAR CORREO: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

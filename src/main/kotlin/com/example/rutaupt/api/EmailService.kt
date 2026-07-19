package com.example.rutaupt.api

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmailService {
    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val smtpHost = System.getenv("SMTP_HOST")?.trim() ?: "smtp.gmail.com"
        val smtpPort = System.getenv("SMTP_PORT")?.trim() ?: "465"
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
            put("mail.smtp.ssl.trust", smtpHost)
            
            // Lógica para Puerto 465 (SSL Directo) - Recomendado para Railway
            if (smtpPort == "465") {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.port", smtpPort)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            } else {
                // Lógica para Puerto 587 (STARTTLS)
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            put("mail.smtp.connectiontimeout", "15000") 
            put("mail.smtp.timeout", "15000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtpUser, smtpPass)
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "RutaUPT Soporte"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de credenciales – RutaUPT"
                setText("Hola $name, tus credenciales de acceso para RutaUPT son: Email: $to | Contraseña: $password")
            }
            
            println("Iniciando envío a $to usando puerto $smtpPort...")
            Transport.send(message)
            println("¡Correo enviado con éxito!")
            true
        } catch (e: Exception) {
            println("FALLO CRÍTICO SMTP: ${e.message}")
            false
        }
    }
}

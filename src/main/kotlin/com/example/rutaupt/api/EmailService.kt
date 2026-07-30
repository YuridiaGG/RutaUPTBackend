package com.example.rutaupt.api

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmailService {
    init {
        // Forza el uso de IPv4 para evitar problemas de conexión en entornos como Railway
        System.setProperty("java.net.preferIPv4Stack", "true")
    }

    suspend fun sendPasswordRecoveryEmail(name: String, to: String, password: String): Boolean = withContext(Dispatchers.IO) {
        // Usamos googlemail.com como alternativa que a veces resuelve mejor en Railway
        val smtpHost = System.getenv("SMTP_HOST")?.trim() ?: "smtp.googlemail.com"
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
            
            if (smtpPort == "465") {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.port", smtpPort)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            put("mail.smtp.ssl.trust", "*")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.debug", "true") 
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtpUser, smtpPass)
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "RutaUPT Soporte"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de credenciales – RutaUPT"
                setText("Hola $name,\n\nTus credenciales de acceso para RutaUPT son:\n\nEmail: $to\nContraseña: $password\n\nSaludos,\nEquipo RutaUPT")
            }
            
            println("DEBUG: Intentando envío a $to via $smtpHost:$smtpPort")
            Transport.send(message)
            println("¡Correo enviado con éxito!")
            true
        } catch (e: Exception) {
            println("FALLO CRÍTICO SMTP: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

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
            
            // Configuración robusta para Puerto 465 (SSL)
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.socketFactory.port", smtpPort)
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.ssl.trust", smtpHost)
            
            // Timeouts para evitar que el servidor se quede colgado (demora mucho)
            put("mail.smtp.connectiontimeout", "10000") 
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
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
            
            println("Intentando enviar correo a $to por el puerto $smtpPort...")
            Transport.send(message)
            println("¡ÉXITO: Correo enviado correctamente!")
            true
        } catch (e: Exception) {
            println("FALLO CRÍTICO SMTP (Puerto $smtpPort): ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

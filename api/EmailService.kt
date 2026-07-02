package com.example.rutaupt.api

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

object EmailService {
    fun sendPasswordRecoveryEmail(to: String, password: String): Boolean {
        val smtpHost = System.getenv("SMTP_HOST") ?: "smtp.gmail.com"
        val smtpPort = System.getenv("SMTP_PORT") ?: "587"
        val smtpUser = System.getenv("SMTP_USER") ?: ""
        val smtpPass = System.getenv("SMTP_PASS") ?: ""

        if (smtpUser.isEmpty() || smtpPass.isEmpty()) {
            println("Error: Credenciales SMTP no configuradas.")
            return false
        }

        val prop = Properties().apply {
            put("mail.smtp.auth", true)
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

        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Recuperación de contraseña – RutaUPT"
                setText("""
                    Estimado(a) usuario(a) de RutaUPT:

                    Reciba un cordial saludo.

                    Hemos recibido y procesado correctamente su solicitud de recuperación de contraseña. A continuación, se muestran los datos de acceso asociados a su cuenta:

                    Correo electrónico: $to
                    Contraseña: $password

                    Por motivos de seguridad, le recomendamos cambiar su contraseña después de iniciar sesión. Puede hacerlo fácilmente desde la sección "Mi Perfil" dentro de la aplicación RutaUPT, donde encontrará la opción "Cambiar contraseña" para actualizar sus credenciales de forma segura.

                    Si usted no realizó esta solicitud de recuperación, le recomendamos comunicarse con el equipo de soporte de RutaUPT lo antes posible para proteger su cuenta.

                    Agradecemos su confianza en RutaUPT.

                    Atentamente,

                    Equipo de Desarrollo
                    RutaUPT
                    Universidad Politécnica de Tulancingo
                """.trimIndent())
            }
            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

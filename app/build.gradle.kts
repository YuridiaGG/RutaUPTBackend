plugins {alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    application
}

group = "com.example.rutaupt"
version = "1.0.0"

application {
    // Esta es la clase que arranca el servidor
    mainClass.set("com.example.rutaupt.ApplicationKt")
}

dependencies {
    implementation(libs.logback)
    // Servidor Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Base de Datos (Aquí va lo que preguntaste)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)

    // Correo
    implementation(libs.jakarta.mail)
}
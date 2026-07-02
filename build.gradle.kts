plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    application
}

group = "com.example.rutaupt"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

application {
    mainClass.set("com.example.rutaupt.ApplicationKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.rutaupt.ApplicationKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Esta es la forma correcta y perezosa (lazy) de incluir las dependencias en el JAR
    // sin que Gradle falle durante la sincronización inicial.
    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
}

dependencies {
    // Logger
    implementation(libs.logback)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation) 
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)
    
    // Email service
    implementation(libs.jakarta.mail)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.testJunit)
}

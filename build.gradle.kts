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

// Aseguramos que use Java 21 como en el servidor de Railway
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.example.rutaupt.ApplicationKt")
}

tasks.jar {
    // Nombre fijo para que el Procfile lo encuentre siempre
    archiveFileName.set("app.jar")

    manifest {
        attributes["Main-Class"] = "com.example.rutaupt.ApplicationKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Incluimos las dependencias de forma segura
    from(configurations.runtimeClasspath.map { config ->
        config.map { if (it.isDirectory) it else zipTree(it) }
    })

    // CRÍTICO: Excluir archivos de firma que causan SecurityException en Railway
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation) 
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)
    implementation(libs.jakarta.mail)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.testJunit)
}

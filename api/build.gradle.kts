plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-server-core:3.5.2")
    implementation("io.ktor:ktor-server-cio:3.5.2")

    implementation("io.ktor:ktor-server-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")

    implementation("io.insert-koin:koin-ktor:4.2.2")

    implementation("ch.qos.logback:logback-classic:1.6.3")

    implementation("org.jetbrains.exposed:exposed-core:1.5.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.5.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.flywaydb:flyway-core:13.7.0")
    implementation("org.flywaydb:flyway-database-postgresql:13.7.0")

    implementation("io.ktor:ktor-server-routing-openapi:3.5.2")
    implementation("io.ktor:ktor-server-swagger:3.5.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.5.2")
    testImplementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
}

kotlin { jvmToolchain(25) }

tasks.test { useJUnitPlatform() }

application { mainClass.set("br.ufrn.noscroll.ApplicationKt") }

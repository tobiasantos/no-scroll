plugins {
    kotlin("jvm") version "2.4.10"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-server-core:3.5.2")
    implementation("io.ktor:ktor-server-cio:3.5.2")

    implementation("ch.qos.logback:logback-classic:1.6.3")
}

kotlin { jvmToolchain(25) }

tasks.test { useJUnitPlatform() }

application { mainClass.set("br.ufrn.noscroll.ApplicationKt") }

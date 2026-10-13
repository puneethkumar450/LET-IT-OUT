plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
}
repositories { mavenCentral() }
dependencies {
    implementation("io.ktor:ktor-server-netty-jvm:3.3.0")
    implementation("io.ktor:ktor-server-core-jvm:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.google.firebase:firebase-admin:9.7.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test"))
}
application { mainClass.set("com.letitout.server.MainKt") }
kotlin { jvmToolchain(21) }
tasks.test { useJUnitPlatform() }

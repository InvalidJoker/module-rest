plugins {
    id("java")
}

group = "eu.cloudnetservice"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repository.derklaro.dev/releases/")
    maven("https://repository.derklaro.dev/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("eu.cloudnetservice.cloudnet:node:4.0.0-RC9")
    implementation("io.netty:netty5-codec-http:5.0.0.Alpha5")
    implementation("io.netty:netty5-handler:5.0.0.Alpha5")
    implementation("org.projectlombok:lombok:1.18.28")
}

plugins {
    id("java")
    id("eu.cloudnetservice.juppiter") version "0.2.0"
}

group = "eu.cloudnetservice.cloudnet"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repository.derklaro.dev/releases/")
    maven("https://repository.derklaro.dev/snapshots/")
}

dependencies {
    compileOnly("eu.cloudnetservice.cloudnet:node:4.0.0-RC9")
}

moduleJson {
    main = "eu.cloudnetservice.cloudnet.rest.CloudNetRestModule"
    name = "CloudNet-Rest2"
    version = "1.0"
    author = "CloudNetService"
}

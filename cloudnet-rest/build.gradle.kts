plugins {
    id("java")
}

group = "eu.cloudnetservice"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}

plugins {
    id("java-library")
    `maven-publish`
}

dependencies {
    implementation("net.kyori:adventure-text-minimessage:4.25.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.25.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

publishing {
    publications {
        create<MavenPublication>("mavenParser") {
            from(components["java"])
            groupId = "dev.vortex"
            artifactId = "vortex-parser"
            version = rootProject.version.toString()

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}
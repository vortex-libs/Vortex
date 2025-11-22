plugins {
    id("java-library")
}

dependencies {
    implementation("org.yaml:snakeyaml:2.5")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

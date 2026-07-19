plugins {
    id("luxspec.java-conventions")
}

description = "JJWT access token adapter for Luxspec security."

dependencies {
    api(project(":security:security-core"))
    api(project(":config:config-core"))
    implementation("io.jsonwebtoken:jjwt-api")
    runtimeOnly("io.jsonwebtoken:jjwt-impl")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson")
}

plugins {
    id("luxspec.spring-boot-starter-conventions")
}

description = "Aggregate Spring Boot starter for the standard Luxspec MVC stack."

dependencies {
    api(project(":core:core-spring-boot-starter"))
    api(project(":config:config-spring-boot-starter"))
    api(project(":web:web-spring-boot-starter"))
    api(project(":security:security-spring-boot-starter"))
    api(project(":user:user-spring-boot-starter"))
}

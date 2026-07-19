plugins {
    id("luxspec.java-conventions")
}

description = "JPA adapters for Luxspec query and pagination contracts."

dependencies {
    api(project(":data:data-core"))
    api("jakarta.persistence:jakarta.persistence-api")
    testImplementation("org.hibernate.orm:hibernate-core")
    testRuntimeOnly("com.h2database:h2")
}

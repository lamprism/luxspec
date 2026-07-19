/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("luxspec.project")
    id("java-library")
    id("luxspec.dependency-management-conventions")
    id("maven-publish")
}

val javaVersion = providers.gradleProperty("luxspec.java.version")
    .map(String::toInt)
    .get()

val pathSegments = path.removePrefix(":").split(":")
val moduleArtifactId = when {
    project == rootProject -> rootProject.name
    pathSegments == listOf("core", "base") -> "${rootProject.name}-core"
    else -> (listOf(rootProject.name) + pathSegments).joinToString("-")
}

base {
    archivesName.set(moduleArtifactId)
}

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

val kotlinSources = fileTree("src/main/java") {
    include("**/*.kt")
}

if (!kotlinSources.isEmpty) {
    pluginManager.apply("org.jetbrains.kotlin.jvm")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(javaVersion)

        sourceSets.named("main") {
            kotlin.setSrcDirs(emptyList<String>())
        }
    }

    tasks.named<KotlinJvmCompile>("compileKotlin") {
        setSource(kotlinSources)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = moduleArtifactId

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set(moduleArtifactId)
                description.set(providers.provider { project.description ?: "${rootProject.name} ${project.name} module" })
                url.set(providers.gradleProperty("luxspec.pom.url"))

                licenses {
                    license {
                        name.set(providers.gradleProperty("luxspec.pom.license.name"))
                        url.set(providers.gradleProperty("luxspec.pom.license.url"))
                    }
                }

                developers {
                    developer {
                        id.set(providers.gradleProperty("luxspec.pom.developer.id"))
                        name.set(providers.gradleProperty("luxspec.pom.developer.name"))
                    }
                }

                scm {
                    connection.set(providers.gradleProperty("luxspec.pom.scm.connection"))
                    developerConnection.set(providers.gradleProperty("luxspec.pom.scm.developerConnection"))
                    url.set(providers.gradleProperty("luxspec.pom.scm.url"))
                }
            }
        }
    }

    repositories {
        maven {
            name = "localBuild"
            url = uri(layout.buildDirectory.dir("repo"))
        }

        providers.gradleProperty("luxspec.publish.repositoryUrl").orNull?.let { repositoryUrl ->
            maven {
                name = providers.gradleProperty("luxspec.publish.repositoryName").orElse("remote").get()
                url = uri(repositoryUrl)

                credentials {
                    username = providers.gradleProperty("luxspec.publish.username").orNull
                    password = providers.gradleProperty("luxspec.publish.password").orNull
                }
            }
        }
    }
}

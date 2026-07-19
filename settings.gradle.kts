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

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "luxspec"

includeBuild("build-logic")

include(":dependency-management")
project(":dependency-management").projectDir = file("gradle/dependency-management")

listOf(
    "core:base",
    "core:spring",
    "core:spring-boot-autoconfigure",
    "core:spring-boot-starter",
    "data:core",
    "data:jpa",
    "config:core",
    "config:jpa",
    "config:spring-boot-autoconfigure",
    "config:spring-boot-starter",
    "security:core",
    "security:spring",
    "security:jwt-jjwt",
    "security:spring-boot-autoconfigure",
    "security:spring-boot-starter",
    "web:core",
    "web:spring",
    "web:spring-boot-autoconfigure",
    "web:spring-boot-starter",
    "user:core",
    "user:jpa",
    "user:security",
    "user:spring",
    "user:spring-boot-autoconfigure",
    "user:spring-boot-starter",
    "spring-boot-starter"
).forEach { modulePath ->
    include(":$modulePath")
    if (modulePath.contains(':')) {
        project(":$modulePath").name = modulePath.replace(':', '-')
    }
}

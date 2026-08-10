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

plugins {
    id("luxspec.spring-boot-starter-conventions")
}

description = "Aggregate Spring Boot starter for the standard Luxspec MVC stack."

dependencies {
    api(project(":core:core-spring"))
    api(project(":core:core-spring-boot-autoconfigure"))
    api(project(":core:core-cache-spring-boot-autoconfigure"))
    api(project(":config:config-spring-boot-autoconfigure"))
    api(project(":security:security-jwt-nimbus-spring-boot-autoconfigure"))
    api(project(":security:security-spring-boot-autoconfigure"))
    api(project(":audit:audit-spring-boot-autoconfigure"))
    api(project(":web:web-spring-boot-autoconfigure"))
    api(project(":user:user-spring-boot-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-web")
}

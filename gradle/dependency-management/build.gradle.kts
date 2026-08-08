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
    id("luxspec.project")
    `java-platform`
}

description = "Dependency management platform for Luxspec."

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))

    constraints {
        api("org.jspecify:jspecify:1.0.1")
        api("com.github.ben-manes.caffeine:caffeine:3.2.4")
        api("com.github.f4b6a3:ulid-creator:5.2.4")
        api("com.github.luben:zstd-jni:1.5.7-12")
        api("org.bouncycastle:bcprov-jdk18on:1.85")
        api("com.nimbusds:nimbus-jose-jwt:10.9.1")
    }
}

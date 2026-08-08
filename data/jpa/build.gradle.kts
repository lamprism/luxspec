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
    id("luxspec.java-conventions")
}

description = "JPA adapters for Luxspec query and pagination contracts."

dependencies {
    api(project(":core:core-base"))
    api(project(":data:data-core"))
    api("jakarta.persistence:jakarta.persistence-api")
    implementation("com.github.luben:zstd-jni")
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor")
    implementation("org.slf4j:slf4j-api")
    testImplementation("org.hibernate.orm:hibernate-core")
    testRuntimeOnly("com.h2database:h2")
}

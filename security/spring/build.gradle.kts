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
    id("luxspec.spring-conventions")
}

description = "Spring Security integration for Luxspec security."

dependencies {
    api(project(":security:security-core"))
    api("jakarta.servlet:jakarta.servlet-api")
    api("org.springframework.security:spring-security-core")
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    testImplementation("org.springframework:spring-test")
}

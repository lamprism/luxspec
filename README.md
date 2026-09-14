<div align="center">

# Luxspec

**Explicit, modular building blocks for Java backend systems.**

[![Verify](https://github.com/lamprism/luxspec/actions/workflows/verify.yml/badge.svg)](https://github.com/lamprism/luxspec/actions/workflows/verify.yml)
[![Java 17](https://img.shields.io/badge/Java-17-437291.svg)](https://adoptium.net/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

</div>

Luxspec is a modular Java library for backend applications that need clear boundaries between domain contracts, runtime
behavior, and framework integrations. It provides reusable building blocks for execution context, configuration, data
access, databases, security, users, web responses, console commands, audit events, and observability.

> **Development status:** The current project version is `0.1.0-SNAPSHOT`. The public API is under active development
> and may change before the first stable release.

## Why Luxspec

- **Provider-independent contracts:** Keep application-facing APIs separate from persistence and web frameworks.
- **Explicit behavior:** Represent configuration, pagination, authorization, failures, and lifecycle events as typed
  contracts.
- **Composable modules:** Depend on the smallest capability you need and add framework integrations separately.
- **Java 17 baseline:** Use modern Java features in a library suitable for mixed Java and Kotlin builds.

## Capabilities

| Area                    | Summary                                                                                                                  |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Core                    | Execution context, resources, events, cache contracts, errors, validation, and message resolution.                       |
| Configuration           | Typed definitions, sources, codecs, validation, layered resolution, caching, events, TOML, and JPA adapters.             |
| Data                    | Query and pagination contracts with JPA Criteria adapters.                                                               |
| Database                | JDBC settings, connection assembly, HikariCP integration, and Spring Boot auto-configuration.                            |
| Security and users      | Authentication, authorization, request firewalls, JWT, token lifecycle, users, roles, and passwords.                     |
| Web and console         | HTTP response models, collection pagination, Spring MVC adapters, command catalogs, and execution sessions.              |
| Audit and observability | Audit event translation, metrics and observations, runtime registries, Prometheus, Micrometer, and Spring Boot adapters. |

## Quick Start

Luxspec artifacts use the Maven group `com.lamprism.luxspec`. Replace `VERSION` with the release version you want to
consume.

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.lamprism.luxspec:luxspec-core:VERSION")
}
```

For the standard Spring Boot MVC stack, use `luxspec-spring-boot-starter:VERSION`. Other artifacts are listed below.

### Maven

```xml

<dependency>
    <groupId>com.lamprism.luxspec</groupId>
    <artifactId>luxspec-core</artifactId>
    <version>VERSION</version>
</dependency>
```

### Snapshot artifacts

Snapshot artifacts are published separately from releases. Add the snapshot repository and use `0.1.0-SNAPSHOT`:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "centralSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    implementation("com.lamprism.luxspec:luxspec-core:0.1.0-SNAPSHOT")
}
```

For Maven:

```xml

<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>com.lamprism.luxspec</groupId>
    <artifactId>luxspec-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Modules

The following consumer artifacts are grouped by capability. Artifact IDs use the `luxspec-` prefix; `luxspec-core` is
the special artifact for the core base module.

| Area          | Artifacts                                                                                                                                                                                                                             |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core          | `luxspec-core`, `luxspec-core-spring`, `luxspec-core-spring-boot-autoconfigure`, `luxspec-core-cache-spring-boot-autoconfigure`                                                                                                       |
| Data          | `luxspec-data-jpa`, `luxspec-data-jpa-spring-boot-autoconfigure`                                                                                                                                                                      |
| Database      | `luxspec-database-core`, `luxspec-database-hikari`, `luxspec-database-spring-boot-autoconfigure`                                                                                                                                      |
| Configuration | `luxspec-config-core`, `luxspec-config-jpa`, `luxspec-config-jpa-spring-boot-autoconfigure`, `luxspec-config-spring-boot-autoconfigure`                                                                                               |
| Security      | `luxspec-security-core`, `luxspec-security-spring`, `luxspec-security-jwt-nimbus`, `luxspec-security-jwt-nimbus-spring-boot-autoconfigure`, `luxspec-security-spring-boot-autoconfigure`                                              |
| Audit         | `luxspec-audit-core`, `luxspec-audit-integration`, `luxspec-audit-spring-boot-autoconfigure`                                                                                                                                          |
| Web and users | `luxspec-web-core`, `luxspec-web-spring`, `luxspec-web-spring-boot-autoconfigure`, `luxspec-user-core`, `luxspec-user-security`, `luxspec-user-spring`, `luxspec-user-spring-boot-autoconfigure`                                      |
| Console       | `luxspec-console-core`                                                                                                                                                                                                                |
| Observability | `luxspec-observability-core`, `luxspec-observability-runtime`, `luxspec-observability-prometheus`, `luxspec-observability-micrometer`, `luxspec-observability-spring-boot-autoconfigure`, `luxspec-observability-spring-boot-starter` |
| Aggregate     | `luxspec-spring-boot-starter`                                                                                                                                                                                                         |

## Compatibility

| Component           | Current baseline                        |
|---------------------|-----------------------------------------|
| Java                | 17                                      |
| Spring Boot modules | Spring Boot `4.1.0` dependency platform |

Framework-specific modules may introduce additional requirements. Use only the core artifacts when an application does
not need the corresponding adapter.

## Build From Source

Requirements:

- JDK 17 or a compatible Java 17 toolchain.
- A network connection for the first dependency resolution.

Build and test all modules:

```bash
./gradlew build
```

On Windows, use `gradlew.bat build`.

Run a focused module test:

```bash
./gradlew :core:core-base:test
```

Publish local artifacts for another local build:

```bash
./gradlew publishToMavenLocal
```

## Contributing

Before opening a pull request:

1. Keep changes focused and preserve the dependency direction between contracts and adapters.
2. Add or update tests for behavior that changes.
3. Run `./gradlew build` locally.

## License

Luxspec is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

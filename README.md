<div align="center">

# Luxspec

**Explicit, modular building blocks for Java backend systems.**

[![Verify](https://github.com/lamprism/luxspec/actions/workflows/verify.yml/badge.svg)](https://github.com/lamprism/luxspec/actions/workflows/verify.yml)
[![Java 17](https://img.shields.io/badge/Java-17-437291.svg)](https://adoptium.net/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

</div>

Luxspec is a modular Java library for backend applications that need clear boundaries between domain contracts, runtime
behavior, and framework integrations. It provides provider-independent capabilities for typed configuration, query and
pagination models, execution context, resources, security, users, and web responses, with optional Spring, JPA, Servlet,
Nimbus JWT, and Spring Boot adapters.

> **Development status:** The current project version is `0.1.0-SNAPSHOT`. The public API is under active development
> and may change before the first stable release.

## Why Luxspec

- **Provider-independent contracts:** Keep domain-facing APIs free from persistence and web framework types wherever the
  module boundary allows it.
- **Explicit behavior:** Represent origins, pagination modes, authorization decisions, configuration policies, and
  failure categories as typed contracts instead of hidden sentinel values.
- **Composable modules:** Depend on the smallest capability you need and add Spring, JPA, security, or Boot integration
  separately.
- **Safe extension points:** Replace default providers and adapters without coupling application code to Caffeine,
  Nimbus JWT, Spring MVC, or a particular persistence implementation.
- **Java 17 baseline:** Use modern Java language and library features while remaining suitable for mixed Java and Kotlin
  builds.

## Capabilities

| Area          | What it provides                                                                                                                                                                                                                  |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core          | Immutable scoped execution context, typed resource identity and provider registration, synchronous events, cache contracts, error codes, and message resolution contracts.                                                        |
| Data          | Typed query fields and expressions, explicit complete/page/slice results, ordering, query windows, validation limits, and JPA Criteria translation.                                                                               |
| Configuration | Typed `ConfigSpec<T>` definitions, parameter binding, codecs, validation, layered sources, operation-aware policies, fallback and origin tracking, cache coordination, and value-free change events.                              |
| Security      | Authentication dispatch, subjects and grants, authorization scopes and resource actions, fail-closed request firewalls, cryptographic key-set contracts, access tokens, refresh-token lifecycle roles, and revocation boundaries. |
| Users         | User and role contracts, lifecycle state, user browsing, password schemes, and user-to-security adapters. User persistence remains application-owned.                                                                             |
| Web           | Provider-independent HTTP response and collection representation models, explicit pagination metadata, and Spring MVC adapters.                                                                                                   |
| Spring Boot   | Focused auto-configuration modules for core, cache, configuration, data, security, users, and web utilities, plus one aggregate MVC starter.                                                                                      |

## Architecture

Luxspec separates contracts from adapters. Core modules own reusable types and rules; integration modules own framework
and persistence concerns.

```text
Application code
    |
    +--> Provider-independent contracts
    |       core, data, config, security, user, web
    |
    +--> Optional adapters
            Spring, Spring Boot, JPA, Servlet, Nimbus JWT, Caffeine
```

The intended dependency direction is from application-facing adapters toward provider-independent contracts. A core
module should not require an application to adopt Spring MVC, Spring Data, or a specific token implementation merely to
use its domain model.

## Installation

Luxspec artifacts use the Maven group `com.lamprism.luxspec`. Replace `VERSION` with the release you want to consume.

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.lamprism.luxspec:luxspec-core:VERSION")
}
```

For the standard Spring Boot MVC stack, use the aggregate starter:

```kotlin
dependencies {
    implementation("com.lamprism.luxspec:luxspec-spring-boot-starter:VERSION")
}
```

Snapshot artifacts are published separately from releases. When using the current snapshot, add the snapshot repository
and use `0.1.0-SNAPSHOT` explicitly:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    implementation("com.lamprism.luxspec:luxspec-core:0.1.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>com.lamprism.luxspec</groupId>
    <artifactId>luxspec-core</artifactId>
    <version>VERSION</version>
</dependency>
```

## Quick Start

The core context API is immutable and typed. A scope restores the previous context when it closes:

```java
import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;

public final class RequestContextExample {
    private static final ContextKey<String> REQUEST_ID =
            ContextKey.of("requestId", String.class);

    public static void run(String requestId) {
        ExecutionContext context = ExecutionContext.empty()
                .with(REQUEST_ID, requestId);

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context)) {
            String currentRequestId = ExecutionContexts.requireCurrent()
                    .get(REQUEST_ID)
                    .orElseThrow();
            System.out.println(currentRequestId);
        }
    }
}
```

Configuration definitions are similarly independent from their storage provider:

```java
import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigSpec;

public final class ConfigExample {
    private ConfigExample() {
    }

    public static ConfigSpec<String> serviceEndpoint() {
        return ConfigSpec
                .builder("service.endpoint", ConfigCodecs.string())
                .defaultValue("https://api.example.com")
                .build();
    }
}
```

The definition can be resolved through a configured `ConfigReader` or `ConfigProvider` backed by the source adapters
selected by the application.

JPA query adapters use direct same-name entity attributes by default. The Spring Boot JPA adapter exposes a factory, so
an application does not need to create `CriteriaQuery`, `Root`, count queries, or slice look-ahead queries manually:

```java
import com.lamprism.luxspec.data.PageWindow;
import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.jpa.JpaQueryExecutorFactory;

public final class JpaQueryExample {
    private final JpaQueryExecutorFactory queryExecutors;

    public QueryResult<OrderEntity> findOrders(QueryCriteria criteria) {
        return queryExecutors
                .forEntity(OrderEntity.class)
                .query(criteria, new PageWindow(0, 20));
    }
}
```

The application validates criteria with its `QuerySchema` before execution. Renamed attributes, joins, and computed
expressions use an explicit `JpaFieldResolver`; the default resolver uses the JPA Criteria API and does not scan Java
fields through reflection.

## Modules

The published artifact name is shown next to each Gradle project path. `:core:core-base` is published as `luxspec-core`;
the remaining modules follow the `luxspec-<project-name>` naming convention.

### Core and data

| Gradle project                               | Artifact                                       | Purpose                                                                                                |
|----------------------------------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `:core:core-base`                            | `luxspec-core`                                 | Core utilities and provider-independent context, resource, event, cache, error, and message contracts. |
| `:core:core-cache`                           | `luxspec-core-cache`                           | Bounded local cache implementation backed by Caffeine.                                                 |
| `:core:core-spring`                          | `luxspec-core-spring`                          | Spring adapters for core capabilities.                                                                 |
| `:core:core-spring-boot-autoconfigure`       | `luxspec-core-spring-boot-autoconfigure`       | Spring Boot defaults for core Spring adapters, including execution-context task propagation.           |
| `:core:core-cache-spring-boot-autoconfigure` | `luxspec-core-cache-spring-boot-autoconfigure` | Spring Boot properties and default cache assembly for the local Caffeine cache.                        |
| `:data:data-core`                            | `luxspec-data-core`                            | Provider-independent query, filtering, ordering, and pagination contracts.                             |
| `:data:data-jpa`                             | `luxspec-data-jpa`                             | JPA Criteria adapters for data contracts.                                                              |
| `:data:data-jpa-spring-boot-autoconfigure`   | `luxspec-data-jpa-spring-boot-autoconfigure`   | Spring Boot defaults for JPA Criteria translation and the application-managed query executor factory.  |

### Configuration

| Gradle project                                 | Artifact                                       | Purpose                                                                                                         |
|------------------------------------------------|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `:config:config-core`                          | `luxspec-config-core`                          | Typed configuration definitions, sources, policies, providers, resolution, caching, events, and TOML support.   |
| `:config:config-jpa`                           | `luxspec-config-jpa`                           | JPA-backed configuration source with one-row value payload storage and an explicitly included Liquibase schema. |
| `:config:config-jpa-spring-boot-autoconfigure` | `luxspec-config-jpa-spring-boot-autoconfigure` | Conditional JPA Config source assembly when the application supplies its repository.                            |
| `:config:config-spring-boot-autoconfigure`     | `luxspec-config-spring-boot-autoconfigure`     | Spring Boot auto-configuration for configuration providers.                                                     |

### Security

| Gradle project                                            | Artifact                                                | Purpose                                                                                                          |
|-----------------------------------------------------------|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `:security:security-core`                                 | `luxspec-security-core`                                 | Framework-independent authentication, authorization, firewall, cryptography, token, and refresh-token contracts. |
| `:security:security-spring`                               | `luxspec-security-spring`                               | Spring Security and Servlet integration.                                                                         |
| `:security:security-jwt-nimbus`                           | `luxspec-security-jwt-nimbus`                           | Nimbus-backed access-token issuance and verification.                                                            |
| `:security:security-jwt-nimbus-spring-boot-autoconfigure` | `luxspec-security-jwt-nimbus-spring-boot-autoconfigure` | Explicitly enabled Config-backed Nimbus JWT access-token assembly.                                               |
| `:security:security-spring-boot-autoconfigure`            | `luxspec-security-spring-boot-autoconfigure`            | Spring Boot auto-configuration for security integration.                                                         |

### Web and users

| Gradle project                         | Artifact                                 | Purpose                                                                                       |
|----------------------------------------|------------------------------------------|-----------------------------------------------------------------------------------------------|
| `:web:web-core`                        | `luxspec-web-core`                       | Provider-independent HTTP response and collection representation contracts.                   |
| `:web:web-spring`                      | `luxspec-web-spring`                     | Spring MVC adapters for web capabilities.                                                     |
| `:web:web-spring-boot-autoconfigure`   | `luxspec-web-spring-boot-autoconfigure`  | Spring Boot auto-configuration for web capabilities.                                          |
| `:user:user-core`                      | `luxspec-user-core`                      | User, role, lifecycle, provider, and browser contracts.                                       |
| `:user:user-security`                  | `luxspec-user-security`                  | Adapters between user and security modules, including password protection and grant assembly. |
| `:user:user-spring`                    | `luxspec-user-spring`                    | Spring adapters for user capabilities.                                                        |
| `:user:user-spring-boot-autoconfigure` | `luxspec-user-spring-boot-autoconfigure` | Spring Boot auto-configuration for user capabilities.                                         |

### Aggregate

| Gradle project         | Artifact                      | Purpose                                                           |
|------------------------|-------------------------------|-------------------------------------------------------------------|
| `:spring-boot-starter` | `luxspec-spring-boot-starter` | Aggregate starter for the standard Luxspec Spring Boot MVC stack. |

## Compatibility

| Component           | Current baseline                                             |
|---------------------|--------------------------------------------------------------|
| Java                | 17                                                           |
| Build               | Gradle Wrapper with Kotlin DSL build logic                   |
| Spring Boot modules | Spring Boot `4.0.7` dependency platform in the current build |
| License             | Apache License 2.0                                           |

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

Run a focused module test:

```bash
./gradlew :core:core-base:test
```

Publish local artifacts for another local build:

```bash
./gradlew publishToMavenLocal
```

## Contributing

Contributions are welcome. Before opening a pull request:

1. Keep changes focused and preserve the dependency direction between core contracts and adapters.
2. Add or update tests for behavior that changes.
3. Run `./gradlew build` locally.
4. Update the README or architecture documentation when public module behavior changes.

Please open an issue for a larger API or architecture change before implementing it so the module boundary and
compatibility impact can be discussed early.

## License

Luxspec is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

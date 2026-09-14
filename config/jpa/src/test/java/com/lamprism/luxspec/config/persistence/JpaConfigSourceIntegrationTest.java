package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaConfigSourceIntegrationTest {
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void openPersistenceContext() throws Exception {
        String databaseUrl = databaseUrl();
        JdbcDataSource dataSource = dataSource(databaseUrl);
        applyChangelog(dataSource);
        entityManagerFactory = Persistence.createEntityManagerFactory(
                "luxspec-config-test",
                Map.of("jakarta.persistence.jdbc.url", databaseUrl)
        );
        entityManager = entityManagerFactory.createEntityManager();
    }

    @AfterEach
    void closePersistenceContext() {
        if (entityManager != null) {
            entityManager.close();
        }
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void isolatesEntriesWithTheSameKeyAcrossSourcePartitions() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC);
        JpaConfigEntryRepository repository = new EntityManagerJpaConfigEntryRepository(entityManager);
        ConfigKey key = ConfigKey.of("feature.enabled");
        JpaConfigSource firstSource = new JpaConfigSource(ConfigSourceId.of("first"), repository, clock);
        JpaConfigSource secondSource = new JpaConfigSource(ConfigSourceId.of("second"), repository, clock);

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            firstSource.set(key, RawConfigValue.string("true"));
            secondSource.set(key, RawConfigValue.string("false"));
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();

        assertEquals("true", firstSource.get(key).requireRawValue().requireScalar());
        assertEquals("false", secondSource.get(key).requireRawValue().requireScalar());
    }

    @Test
    void preservesRawScalarKindsAndListElementBoundaries() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC);
        JpaConfigEntryRepository repository = new EntityManagerJpaConfigEntryRepository(entityManager);
        JpaConfigSource source = new JpaConfigSource(ConfigSourceId.of("database"), repository, clock);
        ConfigKey integerKey = ConfigKey.of("size.integer");
        ConfigKey textKey = ConfigKey.of("size.text");
        ConfigKey decimalKey = ConfigKey.of("ratio");
        ConfigKey listKey = ConfigKey.of("sizes");

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            source.set(integerKey, RawConfigValue.integer(1000));
            source.set(textKey, RawConfigValue.string("1KB"));
            source.set(decimalKey, RawConfigValue.decimal(new BigDecimal("1.25")));
            source.set(listKey, RawConfigValue.list(List.of(
                    RawConfigValue.integer(1000),
                    RawConfigValue.string("1KB")
            )));
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();

        assertEquals(RawConfigValue.ScalarKind.INTEGER, source.get(integerKey).requireRawValue().getScalarKind());
        assertEquals(RawConfigValue.ScalarKind.STRING, source.get(textKey).requireRawValue().getScalarKind());
        assertEquals(RawConfigValue.ScalarKind.DECIMAL, source.get(decimalKey).requireRawValue().getScalarKind());
        assertEquals(
                List.of(
                        RawConfigValue.integer(1000),
                        RawConfigValue.string("1KB")
                ),
                source.get(listKey).requireRawValue().requireList()
        );
    }

    @Test
    void persistsAnOptionalTombstoneEntry() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC);
        JpaConfigEntryRepository repository = new EntityManagerJpaConfigEntryRepository(entityManager);
        JpaConfigSource source = new JpaConfigSource(ConfigSourceId.of("database"), repository, clock);
        ConfigKey key = ConfigKey.of("feature.enabled");

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            source.writeTombstone(key);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();

        assertEquals(ConfigEntry.State.TOMBSTONE, source.get(key).getState());
    }

    @Test
    void removesMissingEntriesWithoutFailing() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC);
        JpaConfigEntryRepository repository = new EntityManagerJpaConfigEntryRepository(entityManager);
        JpaConfigSource source = new JpaConfigSource(ConfigSourceId.of("database"), repository, clock);

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            source.remove(ConfigKey.of("missing"));
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }

        assertEquals(ConfigEntry.State.ABSENT, source.get(ConfigKey.of("missing")).getState());
    }

    @Test
    void rejectsIdentifiersThatExceedTheCanonicalStorageLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JpaConfigEntryId.of(ConfigSourceId.of("s".repeat(129)), ConfigKey.of("feature.enabled"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> JpaConfigEntryId.of(ConfigSourceId.of("source"), ConfigKey.of("k".repeat(513)))
        );
    }

    private static String databaseUrl() {
        String databaseName = "luxspec_config_test_" + UUID.randomUUID().toString().replace("-", "");
        return "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
    }

    private static JdbcDataSource dataSource(String databaseUrl) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(databaseUrl);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void applyChangelog(JdbcDataSource dataSource) throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:META-INF/luxspec/liquibase/config/changelog.yaml");
        liquibase.afterPropertiesSet();
    }

}

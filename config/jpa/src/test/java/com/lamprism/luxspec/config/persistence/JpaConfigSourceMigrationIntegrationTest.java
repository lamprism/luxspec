package com.lamprism.luxspec.config.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSourceId;
import com.lamprism.luxspec.config.RawConfigValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaConfigSourceMigrationIntegrationTest {
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
        EntityManagerRepository repository = new EntityManagerRepository();
        ConfigKey key = ConfigKey.of("feature.enabled");
        JpaConfigSource firstSource = new JpaConfigSource(ConfigSourceId.of("first"), repository, clock);
        JpaConfigSource secondSource = new JpaConfigSource(ConfigSourceId.of("second"), repository, clock);

        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            firstSource.set(key, RawConfigValue.scalar("true"));
            secondSource.set(key, RawConfigValue.scalar("false"));
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

    private final class EntityManagerRepository implements JpaConfigEntryRepository {
        @Override
        public Optional<JpaConfigEntry> findById(JpaConfigEntryId id) {
            return Optional.ofNullable(entityManager.find(JpaConfigEntry.class, id));
        }

        @Override
        public JpaConfigEntry save(JpaConfigEntry entry) {
            entityManager.persist(entry);
            return entry;
        }

        @Override
        public void deleteById(JpaConfigEntryId id) {
            JpaConfigEntry entry = entityManager.find(JpaConfigEntry.class, id);
            if (entry != null) {
                entityManager.remove(entry);
            }
        }
    }
}

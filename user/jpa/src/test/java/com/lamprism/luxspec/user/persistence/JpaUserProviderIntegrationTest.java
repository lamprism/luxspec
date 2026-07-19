package com.lamprism.luxspec.user.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lamprism.luxspec.data.OrderBy;
import com.lamprism.luxspec.data.PageResult;
import com.lamprism.luxspec.data.PageWindow;
import com.lamprism.luxspec.data.QueryCondition;
import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.SliceResult;
import com.lamprism.luxspec.data.SliceWindow;
import com.lamprism.luxspec.data.UnboundedWindow;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserQueryFields;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.security.EncodedPassword;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaUserProviderIntegrationTest {
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void openPersistenceContext() throws Exception {
        String databaseUrl = databaseUrl();
        JdbcDataSource dataSource = dataSource(databaseUrl);
        applyChangelog(dataSource);
        entityManagerFactory = Persistence.createEntityManagerFactory(
                "luxspec-user-test",
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
    void browsesACompletePageAndAllowsAnEmptyPageBeyondTheTotal() {
        persistUsers("cyd", "ada", "bea");
        JpaUserProvider provider = new JpaUserProvider(new BrowsingRepository(), entityManager);
        QueryCriteria criteria = activeUsersOrderedByUsername();

        QueryResult<User> firstResult = provider.browse(criteria, new PageWindow(0L, 2));
        QueryResult<User> beyondResult = provider.browse(criteria, new PageWindow(10L, 2));

        PageResult<?> firstPage = assertInstanceOf(PageResult.class, firstResult);
        PageResult<?> beyondPage = assertInstanceOf(PageResult.class, beyondResult);
        assertEquals(List.of("ada", "bea"), firstResult.getItems().stream().map(User::username).toList());
        assertEquals(3L, firstPage.total());
        assertTrue(beyondResult.getItems().isEmpty());
        assertEquals(3L, beyondPage.total());
    }

    @Test
    void createsASliceWithContinuationAndRejectsUnboundedBrowsing() {
        persistUsers("ada", "bea");
        JpaUserProvider provider = new JpaUserProvider(new BrowsingRepository(), entityManager);
        QueryCriteria criteria = activeUsersOrderedByUsername();

        QueryResult<User> result = provider.browse(criteria, new SliceWindow(0L, 1));
        SliceResult<?> slice = assertInstanceOf(SliceResult.class, result);

        assertEquals(List.of("ada"), result.getItems().stream().map(User::username).toList());
        assertTrue(slice.hasNext());
        assertFalse(result.getItems().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> provider.browse(criteria, UnboundedWindow.getInstance()));
    }

    @Test
    void rejectsValuesThatExceedTheCanonicalStorageLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UserEntity.register("a".repeat(121), null, Set.of(Role.USER), Instant.now())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> UserEntity.register("ada", "e".repeat(321), Set.of(Role.USER), Instant.now())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> UserEntity.register("ada", null, Set.of(Role.of("a".repeat(129))), Instant.now())
        );
    }

    @Test
    void readsOptionalPasswordsAndUsesCompareAndReplace() {
        persistUsers("ada");
        UserEntity entity = entityManager.createQuery(
                        "select user from UserEntity user where user.username = :username",
                        UserEntity.class
                )
                .setParameter("username", "ada")
                .getSingleResult();
        long userId = entity.toUser().id();
        JpaUserPasswordStore store = new JpaUserPasswordStore(
                new PersistenceRepository(),
                Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC)
        );
        EncodedPassword currentPassword = new EncodedPassword("old");
        EncodedPassword replacementPassword = new EncodedPassword("new");

        assertTrue(store.find(userId).isEmpty());
        setStoredPassword(userId, currentPassword.getValue());
        assertEquals(currentPassword.getValue(), store.find(userId).orElseThrow().getValue());

        assertTrue(replacePassword(store, userId, currentPassword, replacementPassword));
        assertFalse(replacePassword(store, userId, currentPassword, new EncodedPassword("other")));
        entityManager.clear();
        assertEquals(replacementPassword.getValue(), store.find(userId).orElseThrow().getValue());
    }

    private QueryCriteria activeUsersOrderedByUsername() {
        return new QueryCriteria(
                QueryCondition.equal(UserQueryFields.STATUS, UserStatus.ACTIVE),
                List.of(OrderBy.ascending(UserQueryFields.USERNAME))
        );
    }

    private void persistUsers(String... usernames) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (String username : usernames) {
                entityManager.persist(UserEntity.register(
                        username,
                        null,
                        Set.of(Role.USER),
                        Instant.parse("2026-07-12T00:00:00Z")
                ));
            }
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();
    }

    private void setStoredPassword(long userId, String password) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.createQuery(
                            "update UserEntity user set user.password = :password where user.id = :userId"
                    )
                    .setParameter("password", password)
                    .setParameter("userId", userId)
                    .executeUpdate();
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
        entityManager.clear();
    }

    private boolean replacePassword(
            JpaUserPasswordStore store,
            long userId,
            EncodedPassword currentPassword,
            EncodedPassword replacementPassword
    ) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            boolean replaced = store.replace(userId, currentPassword, replacementPassword);
            transaction.commit();
            entityManager.clear();
            return replaced;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        }
    }

    private static String databaseUrl() {
        String databaseName = "luxspec_user_test_" + UUID.randomUUID().toString().replace("-", "");
        return "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
    }

    private static JdbcDataSource dataSource(String databaseUrl) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(databaseUrl);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private final class PersistenceRepository implements UserRepository {
        @Override
        public UserEntity save(UserEntity user) {
            return entityManager.merge(user);
        }

        @Override
        public Optional<UserEntity> findById(Long id) {
            return Optional.ofNullable(entityManager.find(UserEntity.class, id));
        }

        @Override
        public List<UserEntity> findByIdIn(Collection<Long> ids) {
            if (ids.isEmpty()) {
                return List.of();
            }
            return entityManager.createQuery(
                            "select user from UserEntity user where user.id in :ids",
                            UserEntity.class
                    )
                    .setParameter("ids", ids)
                    .getResultList();
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            return entityManager.createQuery(
                            "select user from UserEntity user where user.username = :username",
                            UserEntity.class
                    )
                    .setParameter("username", username)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public int replacePassword(
                Long userId,
                String currentPassword,
                String replacementPassword,
                Instant updatedAt
        ) {
            return entityManager.createQuery(
                            "update UserEntity user "
                                    + "set user.password = :replacementPassword, "
                                    + "user.updatedAt = :updatedAt, "
                                    + "user.version = user.version + 1 "
                                    + "where user.id = :userId "
                                    + "and user.password = :currentPassword"
                    )
                    .setParameter("userId", userId)
                    .setParameter("currentPassword", currentPassword)
                    .setParameter("replacementPassword", replacementPassword)
                    .setParameter("updatedAt", updatedAt)
                    .executeUpdate();
        }
    }

    private static void applyChangelog(JdbcDataSource dataSource) throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:META-INF/luxspec/liquibase/user/changelog.yaml");
        liquibase.afterPropertiesSet();
    }

    private static final class BrowsingRepository implements UserRepository {
        @Override
        public UserEntity save(UserEntity user) {
            throw new UnsupportedOperationException("Browsing tests do not use repository writes");
        }

        @Override
        public Optional<UserEntity> findById(Long id) {
            throw new UnsupportedOperationException("Browsing tests do not use repository reads");
        }

        @Override
        public List<UserEntity> findByIdIn(Collection<Long> ids) {
            throw new UnsupportedOperationException("Browsing tests do not use repository reads");
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            throw new UnsupportedOperationException("Browsing tests do not use repository reads");
        }

        @Override
        public int replacePassword(
                Long userId,
                String currentPassword,
                String replacementPassword,
                Instant updatedAt
        ) {
            throw new UnsupportedOperationException("Browsing tests do not use repository writes");
        }
    }
}

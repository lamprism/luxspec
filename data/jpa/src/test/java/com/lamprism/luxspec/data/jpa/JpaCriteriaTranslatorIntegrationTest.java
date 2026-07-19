package com.lamprism.luxspec.data.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lamprism.luxspec.data.ComparisonCondition;
import com.lamprism.luxspec.data.LikeCondition;
import com.lamprism.luxspec.data.OrderBy;
import com.lamprism.luxspec.data.QueryCondition;
import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryExpressions;
import com.lamprism.luxspec.data.QueryField;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaCriteriaTranslatorIntegrationTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);
    private static final QueryField<Integer> SCORE = QueryField.of("score", Integer.class);

    private final JpaCriteriaTranslator translator = new JpaCriteriaTranslator();
    private final JpaFieldResolver<QueryableItemEntity> fieldResolver = new ItemFieldResolver();

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void openPersistenceContext() {
        entityManagerFactory = Persistence.createEntityManagerFactory("luxspec-data-jpa-test");
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
    void translatesNestedConditionsAndDescendingOrderAgainstJpa() {
        persistItems(
                new QueryableItemEntity("adam", "active", 10),
                new QueryableItemEntity("alicia", "active", 30),
                new QueryableItemEntity("albert", "blocked", 50),
                new QueryableItemEntity("bruno", "active", 100)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpressions.and(List.of(
                        LikeCondition.of(NAME, "a%"),
                        ComparisonCondition.greaterThanOrEqualTo(SCORE, 10),
                        QueryExpressions.not(QueryCondition.equal(CATEGORY, "blocked"))
                )),
                List.of(OrderBy.descending(SCORE))
        );

        List<String> names = query(criteria).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("alicia", "adam"), names);
    }

    @Test
    void translatesMembershipAndDisjunctionAgainstJpa() {
        persistItems(
                new QueryableItemEntity("alpha", "priority", 1),
                new QueryableItemEntity("bravo", "archived", 2),
                new QueryableItemEntity("charlie", "standard", 3),
                new QueryableItemEntity("delta", "archived", 4)
        );
        QueryCriteria criteria = new QueryCriteria(
                QueryExpressions.or(List.of(
                        QueryCondition.in(CATEGORY, List.of("priority", "standard")),
                        QueryCondition.notIn(NAME, List.of("alpha", "charlie", "delta"))
                )),
                List.of(OrderBy.ascending(NAME))
        );

        List<String> names = query(criteria).stream().map(QueryableItemEntity::name).toList();

        assertEquals(List.of("alpha", "bravo", "charlie"), names);
    }

    private List<QueryableItemEntity> query(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<QueryableItemEntity> query = builder.createQuery(QueryableItemEntity.class);
        Root<QueryableItemEntity> root = query.from(QueryableItemEntity.class);
        query.select(root);
        query.where(translator.predicate(builder, root, criteria, fieldResolver));
        List<Order> orders = translator.orders(builder, root, criteria, fieldResolver);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        return entityManager.createQuery(query).getResultList();
    }

    private void persistItems(QueryableItemEntity... items) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (QueryableItemEntity item : items) {
                entityManager.persist(item);
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

    private static final class ItemFieldResolver implements JpaFieldResolver<QueryableItemEntity> {
        @Override
        public <V> Expression<V> resolve(Root<QueryableItemEntity> root, QueryField<V> field) {
            Objects.requireNonNull(root, "root");
            if (NAME.equals(field)) {
                return root.get("name");
            }
            if (CATEGORY.equals(field)) {
                return root.get("category");
            }
            if (SCORE.equals(field)) {
                return root.get("score");
            }
            throw new IllegalArgumentException("Unsupported test query field: " + field.getName());
        }
    }
}

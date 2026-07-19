package com.lamprism.luxspec.user.persistence;

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.data.PageResult;
import com.lamprism.luxspec.data.PageWindow;
import com.lamprism.luxspec.data.QueryComplexityLimits;
import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryField;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.QuerySchema;
import com.lamprism.luxspec.data.QueryWindow;
import com.lamprism.luxspec.data.SliceResult;
import com.lamprism.luxspec.data.SliceWindow;
import com.lamprism.luxspec.data.UnboundedWindow;
import com.lamprism.luxspec.data.jpa.JpaCriteriaTranslator;
import com.lamprism.luxspec.data.jpa.JpaFieldResolver;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserBrowser;
import com.lamprism.luxspec.user.UserProvider;
import com.lamprism.luxspec.user.UserQueryFields;
import com.lamprism.luxspec.user.UserQuerySchema;
import com.lamprism.luxspec.user.UserResourceTypes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves and browses user read models through internal JPA persistence.
 *
 * @author RollW
 */
public final class JpaUserProvider implements UserProvider, UserBrowser {
    private static final long MAXIMUM_OFFSET = 100_000L;
    private static final int MAXIMUM_LIMIT = 100;
    private static final QueryComplexityLimits COMPLEXITY_LIMITS = new QueryComplexityLimits(4, 32, 8, 100, 128);
    private static final QuerySchema QUERY_SCHEMA = UserQuerySchema.standard();

    private final UserRepository repository;
    private final EntityManager entityManager;
    private final JpaCriteriaTranslator criteriaTranslator;
    private final JpaFieldResolver<UserEntity> fieldResolver;

    /**
     * Creates a user provider with its owning repository and persistence context.
     *
     * @param repository the user repository
     * @param entityManager the JPA persistence context
     */
    public JpaUserProvider(UserRepository repository, EntityManager entityManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.criteriaTranslator = new JpaCriteriaTranslator();
        this.fieldResolver = new UserFieldResolver();
    }

    /**
     * Returns the user resource type handled by this provider.
     *
     * @return the user resource type
     */
    @Override
    public ResourceType<Long> getResourceType() {
        return UserResourceTypes.USER;
    }

    @Override
    public User provide(ResourceReference<Long> reference) {
        validate(reference);
        return repository.findById(reference.id()).map(UserEntity::toUser)
                .orElseThrow(() -> new ResourceException(CommonErrorCode.NOT_FOUND, "User was not found"));
    }

    @Override
    public List<User> provide(Collection<ResourceReference<Long>> references) {
        Map<Long, User> users = new LinkedHashMap<>();
        List<Long> ids = references.stream().map(reference -> {
            validate(reference);
            return reference.id();
        }).toList();
        for (UserEntity entity : repository.findByIdIn(ids)) {
            User user = entity.toUser();
            users.put(user.id(), user);
        }
        return references.stream().map(reference -> {
            User user = users.get(reference.id());
            if (user == null) {
                throw new ResourceException(CommonErrorCode.NOT_FOUND, "User was not found");
            }
            return user;
        }).toList();
    }

    @Override
    public User provideByUsername(String username) {
        return repository.findByUsername(username).map(UserEntity::toUser)
                .orElseThrow(() -> new ResourceException(CommonErrorCode.NOT_FOUND, "User was not found"));
    }

    /**
     * Browses users with the standard user-query schema and bounded offset windows.
     *
     * @param criteria the typed user filters and ordering
     * @param window the requested page or slice window
     * @return the user query result
     */
    @Override
    @Transactional(readOnly = true)
    public QueryResult<User> browse(QueryCriteria criteria, QueryWindow window) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryWindow nonNullWindow = Objects.requireNonNull(window, "window");
        QUERY_SCHEMA.validate(nonNullCriteria, COMPLEXITY_LIMITS);
        if (nonNullWindow instanceof PageWindow pageWindow) {
            return browsePage(nonNullCriteria, pageWindow);
        }
        if (nonNullWindow instanceof SliceWindow sliceWindow) {
            return browseSlice(nonNullCriteria, sliceWindow);
        }
        if (nonNullWindow instanceof UnboundedWindow) {
            throw new IllegalArgumentException("Unbounded user browsing is not allowed");
        }
        throw new IllegalArgumentException("Unsupported user query window");
    }

    private PageResult<User> browsePage(QueryCriteria criteria, PageWindow window) {
        validateWindow(window.offset(), window.limit());
        List<User> users = fetch(criteria, window.offset(), window.limit());
        return new PageResult<>(users, window.offset(), window.limit(), count(criteria));
    }

    private SliceResult<User> browseSlice(QueryCriteria criteria, SliceWindow window) {
        validateWindow(window.offset(), window.limit());
        List<User> users = fetch(criteria, window.offset(), window.limit() + 1);
        boolean hasNext = users.size() > window.limit();
        if (hasNext) {
            users = List.copyOf(users.subList(0, window.limit()));
        }
        return new SliceResult<>(users, window.offset(), window.limit(), hasNext);
    }

    private List<User> fetch(QueryCriteria criteria, long offset, int maximumResults) {
        TypedQuery<UserEntity> query = createUserQuery(criteria);
        query.setFirstResult((int) offset);
        query.setMaxResults(maximumResults);
        return query.getResultList().stream().map(UserEntity::toUser).toList();
    }

    private TypedQuery<UserEntity> createUserQuery(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserEntity> query = builder.createQuery(UserEntity.class);
        Root<UserEntity> root = query.from(UserEntity.class);
        query.where(criteriaTranslator.predicate(builder, root, criteria, fieldResolver));
        List<Order> orders = criteriaTranslator.orders(builder, root, criteria, fieldResolver);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        return entityManager.createQuery(query);
    }

    private long count(QueryCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<UserEntity> root = query.from(UserEntity.class);
        query.select(builder.count(root));
        query.where(criteriaTranslator.predicate(builder, root, criteria, fieldResolver));
        return entityManager.createQuery(query).getSingleResult();
    }

    private void validateWindow(long offset, int limit) {
        if (offset > MAXIMUM_OFFSET) {
            throw new IllegalArgumentException("User query offset exceeds the maximum");
        }
        if (limit > MAXIMUM_LIMIT) {
            throw new IllegalArgumentException("User query limit exceeds the maximum");
        }
    }

    private void validate(ResourceReference<Long> reference) {
        if (!UserResourceTypes.USER.equals(reference.resourceType())) {
            throw new IllegalArgumentException("Reference is not a user reference");
        }
    }

    private static final class UserFieldResolver implements JpaFieldResolver<UserEntity> {
        @Override
        public <V> Expression<V> resolve(Root<UserEntity> root, QueryField<V> field) {
            Objects.requireNonNull(root, "root");
            if (UserQueryFields.ID.equals(field)) {
                return root.get("id");
            }
            if (UserQueryFields.USERNAME.equals(field)) {
                return root.get("username");
            }
            if (UserQueryFields.EMAIL.equals(field)) {
                return root.get("email");
            }
            if (UserQueryFields.STATUS.equals(field)) {
                return root.get("status");
            }
            if (UserQueryFields.REGISTERED_AT.equals(field)) {
                return root.get("registeredAt");
            }
            if (UserQueryFields.UPDATED_AT.equals(field)) {
                return root.get("updatedAt");
            }
            throw new IllegalArgumentException("Unsupported user query field: " + field.getName());
        }
    }
}

package com.lamprism.luxspec.user.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Internal persistence access for the user feature.
 */
public interface UserRepository extends Repository<UserEntity, Long> {
    /**
     * Persists a new or changed user entity.
     *
     * @param user the internal user entity
     * @return the persisted user entity
     */
    UserEntity save(UserEntity user);

    /**
     * Finds one user by its identifier.
     *
     * @param id the user identifier
     * @return the matching user when present
     */
    Optional<UserEntity> findById(Long id);

    /**
     * Finds users for a collection of identifiers.
     *
     * @param ids the requested user identifiers
     * @return the matching user entities
     */
    List<UserEntity> findByIdIn(Collection<Long> ids);

    /**
     * Finds one user by its unique username.
     *
     * @param username the canonical username
     * @return the matching user when present
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Replaces a stored password only when the expected representation still matches.
     *
     * @param userId the user identifier
     * @param currentPassword the representation previously verified by the authenticator
     * @param replacementPassword the newly encoded representation
     * @param updatedAt the password update time
     * @return the number of changed rows
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity user
            set user.password = :replacementPassword,
                user.updatedAt = :updatedAt,
                user.version = user.version + 1
            where user.id = :userId
              and user.password = :currentPassword
            """)
    int replacePassword(
            @Param("userId") Long userId,
            @Param("currentPassword") String currentPassword,
            @Param("replacementPassword") String replacementPassword,
            @Param("updatedAt") Instant updatedAt
    );
}

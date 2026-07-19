package com.lamprism.luxspec.user;

import com.lamprism.luxspec.data.QueryField;

import java.time.Instant;

/**
 * Holds typed field identities supported by ordinary user browsing.
 *
 * @author RollW
 */
public final class UserQueryFields {
    /**
     * The generated account identifier.
     */
    public static final QueryField<Long> ID = QueryField.of("id", Long.class);
    /**
     * The unique account name.
     */
    public static final QueryField<String> USERNAME = QueryField.of("username", String.class);
    /**
     * The optional account email address.
     */
    public static final QueryField<String> EMAIL = QueryField.of("email", String.class);
    /**
     * The mutually exclusive account lifecycle status.
     */
    public static final QueryField<UserStatus> STATUS = QueryField.of("status", UserStatus.class);
    /**
     * The account registration time.
     */
    public static final QueryField<Instant> REGISTERED_AT = QueryField.of("registered-at", Instant.class);
    /**
     * The most recent account update time.
     */
    public static final QueryField<Instant> UPDATED_AT = QueryField.of("updated-at", Instant.class);

    private UserQueryFields() {
    }
}

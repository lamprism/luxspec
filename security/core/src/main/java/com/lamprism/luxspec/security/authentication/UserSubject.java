package com.lamprism.luxspec.security.authentication;


/**
 * Identifies one authenticated user by its stable ID.
 *
 * @author RollW
 */
public final class UserSubject implements Subject {
    private final long userId;

    /**
     * Creates a user subject with a positive stable user identifier.
     *
     * @param userId the stable user identifier
     */
    public UserSubject(long userId) {
        if (userId < 1L) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
    }

    /**
     * Returns the stable numeric user identifier.
     *
     * @return the user identifier
     */
    public long userId() {
        return userId;
    }

    @Override
    public String getType() {
        return "user";
    }

    @Override
    public String getId() {
        return Long.toString(userId);
    }
}

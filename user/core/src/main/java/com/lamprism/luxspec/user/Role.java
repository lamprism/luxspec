package com.lamprism.luxspec.user;

import java.util.Objects;

/**
 * Identifies one extensible user-domain role.
 *
 * @author RollW
 */
public final class Role {
    public static final Role USER = new Role("user");
    public static final Role ADMIN = new Role("admin");
    private final String name;

    /**
     * Creates a role with a canonical lower-kebab identity.
     *
     * @param name the canonical role name
     */
    public Role(String name) {
        this.name = Objects.requireNonNull(name, "name");
        RoleNames.requireCanonical(name);
    }

    /**
     * Returns the canonical role identity.
     *
     * @return the role name
     */
    public String name() {
        return name;
    }

    /**
     * Creates a canonical role value.
     *
     * @param name the canonical role name
     * @return the role value
     */
    public static Role of(String name) {
        return new Role(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Role role)) {
            return false;
        }
        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

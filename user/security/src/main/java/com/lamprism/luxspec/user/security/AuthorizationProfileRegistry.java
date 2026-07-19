package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Assembles profile inheritance and direct scope contributions before runtime grant resolution.
 *
 * @author RollW
 */
public final class AuthorizationProfileRegistry {
    private final Map<AuthorizationProfile, Definition> definitions = new LinkedHashMap<>();

    /**
     * Declares one profile. Repeated equivalent declarations are idempotent.
     *
     * @param profile the profile to declare
     */
    public void register(AuthorizationProfile profile) {
        definitions.putIfAbsent(Objects.requireNonNull(profile, "profile"), new Definition());
    }

    /**
     * Declares that one profile inherits the direct and inherited scopes of another profile.
     *
     * @param profile the inheriting profile
     * @param parent the inherited profile
     */
    public void inherit(AuthorizationProfile profile, AuthorizationProfile parent) {
        AuthorizationProfile nonNullProfile = Objects.requireNonNull(profile, "profile");
        AuthorizationProfile nonNullParent = Objects.requireNonNull(parent, "parent");
        if (nonNullProfile.equals(nonNullParent)) {
            throw new IllegalArgumentException("Authorization profile must not inherit itself");
        }
        definition(nonNullProfile).parents.add(definitionKey(nonNullParent));
    }

    /**
     * Adds concrete scopes directly contributed by one profile.
     *
     * @param profile the contributing profile
     * @param scopes the concrete scopes to contribute
     */
    public void grant(AuthorizationProfile profile, Iterable<AuthorizationScope> scopes) {
        Definition definition = definition(Objects.requireNonNull(profile, "profile"));
        for (AuthorizationScope scope : Objects.requireNonNull(scopes, "scopes")) {
            definition.scopes.add(Objects.requireNonNull(scope, "scope"));
        }
    }

    Set<AuthorizationScope> resolve(AuthorizationProfile profile) {
        return resolve(Objects.requireNonNull(profile, "profile"), new LinkedHashSet<>());
    }

    void validate() {
        for (AuthorizationProfile profile : definitions.keySet()) {
            resolve(profile);
        }
    }

    private Set<AuthorizationScope> resolve(AuthorizationProfile profile, Set<AuthorizationProfile> visiting) {
        if (!visiting.add(profile)) {
            throw new IllegalArgumentException("Authorization profile inheritance contains a cycle");
        }
        Definition definition = definition(profile);
        Set<AuthorizationScope> result = new LinkedHashSet<>(definition.scopes);
        for (AuthorizationProfile parent : definition.parents) {
            result.addAll(resolve(parent, visiting));
        }
        visiting.remove(profile);
        return Set.copyOf(result);
    }

    private Definition definition(AuthorizationProfile profile) {
        Definition definition = definitions.get(profile);
        if (definition == null) {
            throw new IllegalArgumentException("Authorization profile is not registered");
        }
        return definition;
    }

    private AuthorizationProfile definitionKey(AuthorizationProfile profile) {
        definition(profile);
        return profile;
    }

    private static final class Definition {
        private final Set<AuthorizationProfile> parents = new LinkedHashSet<>();
        private final Set<AuthorizationScope> scopes = new LinkedHashSet<>();
    }
}

package com.lamprism.luxspec.user.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.user.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserRoleGrantResolverTest {
    @Test
    void administratorInheritsUserProfileScopesAndFeatureContributions() {
        AuthorizationScope userRead = AuthorizationScope.of("resource:user:read");
        AuthorizationScope userWrite = AuthorizationScope.of("resource:user:write");
        AuthorizationScopeHierarchy hierarchy = AuthorizationScopeHierarchy.builder()
                .declare(userRead)
                .declare(userWrite)
                .build();
        AuthorizationProfileContributor userScopes = registry -> {
            registry.grant(UserAuthorizationProfiles.USER, List.of(userRead));
            registry.grant(UserAuthorizationProfiles.ADMIN, List.of(userWrite));
        };
        UserRoleGrantResolver resolver = new UserRoleGrantResolver(
                UserAuthorizationProfiles.defaultRoleProfiles(),
                List.of(UserAuthorizationProfiles.defaults(), userScopes),
                hierarchy
        );

        assertEquals(
                List.of(userRead),
                resolver.resolve(List.of(Role.USER)).getScopes().stream().sorted((left, right) -> left.name().compareTo(right.name())).toList()
        );
        assertEquals(
                List.of(userRead, userWrite),
                resolver.resolve(List.of(Role.ADMIN)).getScopes().stream().sorted((left, right) -> left.name().compareTo(right.name())).toList()
        );
    }

    @Test
    void rejectsProfileInheritanceCyclesDuringAssembly() {
        AuthorizationProfile first = AuthorizationProfile.of("first");
        AuthorizationProfile second = AuthorizationProfile.of("second");
        AuthorizationProfileContributor cyclic = registry -> {
            registry.register(first);
            registry.register(second);
            registry.inherit(first, second);
            registry.inherit(second, first);
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> new UserRoleGrantResolver(
                        Map.of(),
                        List.of(cyclic),
                        AuthorizationScopeHierarchy.empty()
                )
        );
    }
}

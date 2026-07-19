package com.lamprism.luxspec.user.security;

/**
 * Contributes one cohesive group of profiles, inheritance, or scopes during security assembly.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuthorizationProfileContributor {
    /**
     * Contributes profile definitions to the mutable assembly registry.
     *
     * @param registry the profile assembly registry
     */
    void contribute(AuthorizationProfileRegistry registry);
}

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.resource.Resource;
import com.lamprism.luxspec.resource.ResourceProvider;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Combines resource loading with the fixed authorization pipeline for one resource type.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public final class AuthorizedResourceProvider<ID> {
    private final ResourceProvider<ID> provider;
    private final ResourceAuthorizer<ID> authorizer;
    private final ResourceAccessController accessController;

    /**
     * Creates an authorized view over matching resource provider and authorizer roles.
     *
     * @param provider the resource-loading provider
     * @param authorizer the instance-policy authorizer
     * @param accessController the baseline and instance-policy coordinator
     */
    public AuthorizedResourceProvider(
            ResourceProvider<ID> provider,
            ResourceAuthorizer<ID> authorizer,
            ResourceAccessController accessController
    ) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer");
        this.accessController = Objects.requireNonNull(accessController, "accessController");
        if (!provider.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource provider and authorizer types must match");
        }
    }

    /**
     * Returns the resource type supported by this authorized provider.
     *
     * @return the resource type
     */
    public ResourceType<ID> getResourceType() {
        return provider.getResourceType();
    }

    /**
     * Authorizes and loads one required resource.
     *
     * @param authentication the effective authenticated actor
     * @param action the attempted resource action
     * @param reference the required resource reference
     * @return the loaded resource
     */
    public Resource<ID> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference
    ) {
        requireAllowed(authentication, action, reference);
        return provider.provide(reference);
    }

    /**
     * Authorizes every reference before loading the complete batch in input order.
     *
     * @param authentication the effective authenticated actor
     * @param action the attempted resource action
     * @param references the required resource references
     * @return loaded resources in input order
     */
    public List<? extends Resource<ID>> provide(
            Authentication authentication,
            ResourceAction<ID> action,
            Collection<ResourceReference<ID>> references
    ) {
        List<AuthorizationDecision> decisions = authorizer.authorize(authentication, action, references, accessController);
        for (AuthorizationDecision decision : decisions) {
            if (!decision.isAllowed()) {
                throw new ResourceAccessDeniedException(decision.getReasonCode());
            }
        }
        return provider.provide(references);
    }

    private void requireAllowed(Authentication authentication, ResourceAction<ID> action, ResourceReference<ID> reference) {
        AuthorizationDecision decision = accessController.authorize(authentication, action, reference, authorizer);
        if (!decision.isAllowed()) {
            throw new ResourceAccessDeniedException(decision.getReasonCode());
        }
    }
}

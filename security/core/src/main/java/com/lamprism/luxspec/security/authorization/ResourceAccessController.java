package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.resource.ResourceReference;
import java.util.Objects;

/**
 * Applies baseline scope requirements before resource-instance authorization.
 *
 * @author RollW
 */
public final class ResourceAccessController {
    /**
     * Authorizes one resource action through the fixed baseline and instance-policy sequence.
     *
     * @param authentication the effective authenticated actor
     * @param action the attempted resource action
     * @param reference the referenced resource
     * @param authorizer the authoritative instance-policy evaluator
     * @param <ID> the resource ID type
     * @return the final authorization decision
     */
    public <ID> AuthorizationDecision authorize(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference,
            ResourceAuthorizer<ID> authorizer
    ) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ResourceAction<ID> nonNullAction = Objects.requireNonNull(action, "action");
        ResourceReference<ID> nonNullReference = Objects.requireNonNull(reference, "reference");
        ResourceAuthorizer<ID> nonNullAuthorizer = Objects.requireNonNull(authorizer, "authorizer");
        requireMatchingTypes(nonNullAction, nonNullReference, nonNullAuthorizer);
        if (!nonNullAction.getRequirement().isSatisfiedBy(nonNullAuthentication.grants())) {
            return AuthorizationDecision.denied(AuthErrorCode.PERMISSION_DENIED);
        }
        return Objects.requireNonNull(
                nonNullAuthorizer.authorize(nonNullAuthentication, nonNullAction, nonNullReference),
                "authorization decision"
        );
    }

    private <ID> void requireMatchingTypes(
            ResourceAction<ID> action,
            ResourceReference<ID> reference,
            ResourceAuthorizer<ID> authorizer
    ) {
        if (!action.getResourceType().equals(reference.resourceType())) {
            throw new IllegalArgumentException("Resource action and reference types must match");
        }
        if (!action.getResourceType().equals(authorizer.getResourceType())) {
            throw new IllegalArgumentException("Resource action and authorizer types must match");
        }
    }
}

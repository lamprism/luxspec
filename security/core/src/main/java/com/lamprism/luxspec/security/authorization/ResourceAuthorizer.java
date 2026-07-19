package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Evaluates resource-instance policy after an action's baseline grant requirement passes.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface ResourceAuthorizer<ID> {
    /**
     * Returns the resource type this authorizer owns.
     *
     * @return the owned resource type
     */
    ResourceType<ID> getResourceType();

    /**
     * Applies instance-level policy after baseline requirements have passed.
     *
     * @param authentication the effective authenticated actor
     * @param action the attempted resource action
     * @param reference the referenced resource
     * @return the final instance-level decision
     */
    AuthorizationDecision authorize(
            Authentication authentication,
            ResourceAction<ID> action,
            ResourceReference<ID> reference
    );

    /**
     * Applies the fixed authorization pipeline to each reference in input order.
     *
     * @param authentication the effective authenticated actor
     * @param action the attempted resource action
     * @param references the referenced resources
     * @param accessController the fixed baseline-policy coordinator
     * @return decisions in the same order as references
     */
    default List<AuthorizationDecision> authorize(
            Authentication authentication,
            ResourceAction<ID> action,
            Collection<ResourceReference<ID>> references,
            ResourceAccessController accessController
    ) {
        List<AuthorizationDecision> decisions = new ArrayList<>();
        for (ResourceReference<ID> reference : references) {
            decisions.add(accessController.authorize(authentication, action, reference, this));
        }
        return List.copyOf(decisions);
    }
}

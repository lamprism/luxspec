package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.SecurityContextKeys;
import java.util.Objects;

/**
 * Opens a nested ExecutionContext containing one successful Luxspec Authentication.
 *
 * @author RollW
 */
public final class SpringAuthenticationExecutionContextBridge {
    /**
     * Opens a derived context that exposes the authenticated actor for downstream work.
     *
     * @param authentication the successful Luxspec authentication
     * @return the scope that restores the prior execution context
     */
    public ExecutionContexts.Scope open(Authentication authentication) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ExecutionContext currentContext = ExecutionContexts.current().orElse(ExecutionContext.empty());
        ExecutionContext authenticatedContext = currentContext.get(SecurityContextKeys.AUTHENTICATION).isPresent()
                ? currentContext.replace(SecurityContextKeys.AUTHENTICATION, nonNullAuthentication)
                : currentContext.with(SecurityContextKeys.AUTHENTICATION, nonNullAuthentication);
        return ExecutionContexts.open(authenticatedContext);
    }
}

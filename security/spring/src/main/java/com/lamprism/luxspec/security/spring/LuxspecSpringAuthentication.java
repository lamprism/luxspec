package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Adapts one Luxspec Authentication into Spring Security's authenticated token model.
 *
 * @author RollW
 */
public final class LuxspecSpringAuthentication extends AbstractAuthenticationToken {
    /**
     * The framework-independent authentication adapted for this Spring Security token.
     */
    private final Authentication authentication;

    /**
     * Creates an authenticated Spring Security token from a Luxspec Authentication.
     *
     * @param authentication the framework-independent authentication value
     */
    public LuxspecSpringAuthentication(Authentication authentication) {
        super(authentication.grants().getScopes().stream()
                .map(AuthorizationScope::name)
                .map(SimpleGrantedAuthority::new)
                .toList());
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        setAuthenticated(true);
    }

    /**
     * Returns the underlying framework-independent authentication value.
     *
     * @return the Luxspec authentication
     */
    public Authentication getLuxspecAuthentication() {
        return authentication;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return authentication.subject();
    }

    @Override
    public String getName() {
        return authentication.subject().getId();
    }
}

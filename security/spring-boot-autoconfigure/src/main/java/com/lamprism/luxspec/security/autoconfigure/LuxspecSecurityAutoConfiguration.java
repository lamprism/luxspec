package com.lamprism.luxspec.security.autoconfigure;

import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.spring.LuxspecBearerAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Configures a stateless Luxspec Bearer security chain only when an application has not defined one.
 *
 * @author RollW
 */
@AutoConfiguration(beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityContextHolderFilter.class})
@ConditionalOnBean(AccessTokenAuthenticator.class)
public class LuxspecSecurityAutoConfiguration {
    /**
     * Creates the protected default chain for an application that supplies access-token authentication.
     *
     * @param httpSecurity the servlet security builder
     * @param authenticator the configured Luxspec access-token authenticator
     * @return the stateless protected filter chain
    * @throws Exception when Spring Security cannot build the filter chain
     */
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @Bean
    public SecurityFilterChain luxspecSecurityFilterChain(
            HttpSecurity httpSecurity,
            AccessTokenAuthenticator authenticator
    ) throws Exception {
        AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorized))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .addFilterAfter(
                        new LuxspecBearerAuthenticationFilter(authenticator),
                        SecurityContextHolderFilter.class
                );
        return httpSecurity.build();
    }
}

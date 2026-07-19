package com.lamprism.luxspec.user.autoconfigure;

import com.lamprism.luxspec.user.security.Argon2idPasswordScheme;
import com.lamprism.luxspec.user.security.PasswordScheme;
import com.lamprism.luxspec.user.spring.LuxspecPasswordEncoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Supplies the default provider-independent user password protection and Spring bridge.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass({PasswordScheme.class, PasswordEncoder.class})
public class LuxspecUserAutoConfiguration {
    /**
     * Creates the default Argon2id password scheme when an application has not supplied one.
     *
     * @return the default password scheme
     */
    @Bean
    @ConditionalOnMissingBean(PasswordScheme.class)
    public PasswordScheme passwordScheme() {
        return new Argon2idPasswordScheme();
    }

    /**
     * Creates the Spring Security bridge when an application has not supplied an encoder.
     *
     * @param passwordScheme the configured authoritative password scheme
     * @return the Spring password encoder bridge
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder(PasswordScheme passwordScheme) {
        return new LuxspecPasswordEncoder(passwordScheme);
    }
}

package com.lamprism.luxspec.web.autoconfigure;

import com.lamprism.luxspec.web.spring.DefaultErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.spring.LuxspecExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Registers default MVC error-envelope support when an application has not supplied replacements.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
public class LuxspecWebAutoConfiguration {
    /**
     * Supplies the conservative default mapping from business errors to HTTP status codes.
     *
     * @return the default status resolver
     */
    @Bean
    @ConditionalOnMissingBean(ErrorHttpStatusResolver.class)
    public ErrorHttpStatusResolver errorHttpStatusResolver() {
        return new DefaultErrorHttpStatusResolver();
    }

    /**
     * Supplies the default MVC exception advice for Luxspec exceptions.
     *
     * @param statusResolver the configured error status resolver
     * @return the exception advice
     */
    @Bean
    @ConditionalOnMissingBean(LuxspecExceptionHandler.class)
    public LuxspecExceptionHandler luxspecExceptionHandler(ErrorHttpStatusResolver statusResolver) {
        return new LuxspecExceptionHandler(statusResolver);
    }
}

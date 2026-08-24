package com.lamprism.luxspec.web;

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValueValidator;

/**
 * Typed configuration definitions owned by the Web capability.
 *
 * @author RollW
 */
public final class WebConfigSpec {
    /**
     * The request and response header used for the correlation identifier.
     */
    public static final ConfigSpec<String> CORRELATION_ID_HEADER = ConfigSpec.of(
            "web.correlation-id-header",
            ConfigCodecs.string(),
            "X-Request-ID",
            false,
            ConfigValueValidator.of(
                    value -> !value.isBlank(),
                    "Correlation ID header must not be blank"
            )
    );

    private WebConfigSpec() {
    }
}

package com.lamprism.luxspec.web;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.validation.Validator;

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
            ConfigCodec.string(),
            "X-Request-ID",
            false,
            Validator.of(
                    value -> !value.isBlank(),
                    "Correlation ID header must not be blank"
            )
    );

    private WebConfigSpec() {
    }
}

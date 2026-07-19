package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.ErrorCode;
import org.springframework.http.HttpStatusCode;

/**
 * Maps a provider-independent business error code to an HTTP response status.
 *
 * @author RollW
 */
public interface ErrorHttpStatusResolver {
    /**
     * Resolves the HTTP status for one stable business error.
     *
     * @param errorCode the error to map
     * @return the HTTP status code
     */
    HttpStatusCode resolve(ErrorCode errorCode);
}

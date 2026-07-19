package com.lamprism.luxspec.security.firewall;

import java.util.Objects;

/**
 * Represents framework-independent facts available before authentication.
 *
 * @author RollW
 */
public final class IngressRequest {
    private final String method;
    private final String path;
    private final String clientAddress;

    /**
     * Creates immutable ingress facts supplied by a trusted boundary adapter.
     *
     * @param method the request method
     * @param path the request path
     * @param clientAddress the normalized client address
     */
    public IngressRequest(String method, String path, String clientAddress) {
        this.method = requireText(method, "method");
        this.path = requireText(path, "path");
        this.clientAddress = requireText(clientAddress, "clientAddress");
    }

    /**
     * Returns the request method.
     *
     * @return the request method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the request path.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the normalized client address.
     *
     * @return the client address
     */
    public String getClientAddress() {
        return clientAddress;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}

/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.security.spring.firewall;

import com.lamprism.luxspec.security.firewall.FirewallDecision;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * Responds to denied Firewall decisions with one HTTP status and an optional Retry-After header.
 *
 * @author RollW
 */
public class HttpStatusFirewallDecisionHandler implements FirewallDecisionHandler {
    private final int statusCode;

    /**
     * Creates a handler that returns HTTP 403.
     */
    public HttpStatusFirewallDecisionHandler() {
        this(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Creates a handler with an explicit HTTP status.
     *
     * @param statusCode the HTTP status returned for a denial
     */
    public HttpStatusFirewallDecisionHandler(int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP status");
        }
        this.statusCode = statusCode;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            FirewallDecision decision
    ) throws IOException {
        Objects.requireNonNull(request, "request");
        FirewallDecision nonNullDecision = Objects.requireNonNull(decision, "decision");
        nonNullDecision.getRetryAfter().ifPresent(retryAfter -> response.setHeader(
                "Retry-After",
                Long.toString(retryAfterSeconds(retryAfter))
        ));
        response.sendError(statusCode);
    }

    private static long retryAfterSeconds(Duration duration) {
        long seconds = duration.getSeconds();
        if (duration.getNano() > 0) {
            seconds++;
        }
        return Math.max(1L, seconds);
    }
}

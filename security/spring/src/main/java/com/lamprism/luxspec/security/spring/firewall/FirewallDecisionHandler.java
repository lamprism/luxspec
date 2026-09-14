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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles a denied Firewall decision at the Servlet boundary.
 *
 * @author RollW
 */
@FunctionalInterface
public interface FirewallDecisionHandler {
    /**
     * Handles one denied decision without exposing internal request facts.
     *
     * @param request  the current Servlet request
     * @param response the current Servlet response
     * @param decision the denied Firewall decision
     * @throws IOException      when response writing fails
     * @throws ServletException when the boundary cannot handle the decision
     */
    void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            FirewallDecision decision
    ) throws IOException, ServletException;
}

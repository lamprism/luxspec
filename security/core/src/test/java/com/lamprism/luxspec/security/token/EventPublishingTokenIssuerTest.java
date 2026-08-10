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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.access.AccessToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EventPublishingTokenIssuerTest {
    private static final Instant NOW = Instant.parse("2026-08-09T00:00:00Z");

    @Test
    void publishesACompletedIssueEventWithoutExposingTokenValues() {
        Authentication authentication = new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of())
        );
        TokenIssuance issuance = TokenIssuance.of(List.of(
                IssuedToken.of(
                        new AccessToken("secret-token"),
                        NOW,
                        NOW.plus(Duration.ofMinutes(5))
                )
        ));
        List<Event> events = new ArrayList<>();
        TokenIssuer issuer = new EventPublishingTokenIssuer(
                ignored -> issuance,
                events::add,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertSame(issuance, issuer.issue(authentication));
        assertEquals(1, events.size());
        TokenLifecycleEvent event = (TokenLifecycleEvent) events.get(0);
        assertEquals(TokenLifecycleEvent.Operation.ISSUE, event.getOperation());
        assertEquals(TokenLifecycleEvent.Result.SUCCESS, event.getResult());
        assertEquals(List.of(AccessToken.KIND.getName()), event.getTokenKinds().stream().toList());
        assertEquals(Duration.ZERO, event.getDuration());
    }
}

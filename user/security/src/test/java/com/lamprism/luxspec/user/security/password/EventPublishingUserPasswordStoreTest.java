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

package com.lamprism.luxspec.user.security.password;

import com.lamprism.luxspec.event.Event;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class EventPublishingUserPasswordStoreTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-09T00:00:00Z");

    @Test
    void publishesOnlySuccessfulPasswordReplacementsWithoutPasswordValues() {
        RecordingStore delegate = new RecordingStore();
        List<Event> events = new ArrayList<>();
        EventPublishingUserPasswordStore store = new EventPublishingUserPasswordStore(
                delegate,
                events::add,
                Clock.fixed(OCCURRED_AT, ZoneOffset.UTC)
        );

        delegate.setReplacementResult(false);
        assertFalse(store.replace(7L, new EncodedPassword("old"), new EncodedPassword("new"), PasswordReplacementPurpose.CHANGE));
        assertTrue(events.isEmpty());

        delegate.setReplacementResult(true);
        assertTrue(store.replace(7L, new EncodedPassword("old"), new EncodedPassword("new"), PasswordReplacementPurpose.CHANGE));

        assertEquals(1, events.size());
        UserPasswordChangedEvent event = (UserPasswordChangedEvent) events.get(0);
        assertEquals(7L, event.getUserId());
        assertEquals(UserPasswordChangedEvent.ChangeType.CHANGED, event.getChangeType());
        assertEquals(OCCURRED_AT, event.getOccurredAt());
    }

    @Test
    void distinguishesInitializationRehashAndResetEvents() {
        List<Event> events = new ArrayList<>();
        EventPublishingUserPasswordStore store = new EventPublishingUserPasswordStore(
                new InMemoryUserPasswordStore(), events::add, Clock.fixed(OCCURRED_AT, ZoneOffset.UTC)
        );
        EncodedPassword first = new EncodedPassword("first");
        EncodedPassword rehashed = new EncodedPassword("rehashed");
        assertTrue(store.initialize(7L, first));
        assertFalse(store.initialize(7L, first));
        assertTrue(store.replace(7L, first, rehashed, PasswordReplacementPurpose.REHASH));
        assertTrue(store.reset(7L, new EncodedPassword("reset")));

        assertEquals(List.of(
                UserPasswordChangedEvent.ChangeType.INITIALIZED,
                UserPasswordChangedEvent.ChangeType.REHASHED,
                UserPasswordChangedEvent.ChangeType.RESET
        ), events.stream().map(event -> ((UserPasswordChangedEvent) event).getChangeType()).toList());
    }

    private static final class RecordingStore implements UserPasswordStore {
        private boolean replacementResult;

        @Override
        public Optional<EncodedPassword> find(long userId) {
            return Optional.empty();
        }

        @Override
        public boolean initialize(long userId, EncodedPassword password) {
            return replacementResult;
        }

        @Override
        public boolean replace(
                long userId,
                EncodedPassword currentPassword,
                EncodedPassword replacementPassword,
                PasswordReplacementPurpose purpose
        ) {
            return replacementResult;
        }

        @Override
        public boolean reset(long userId, EncodedPassword password) {
            return replacementResult;
        }

        private void setReplacementResult(boolean replacementResult) {
            this.replacementResult = replacementResult;
        }
    }
}

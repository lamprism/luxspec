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

package com.lamprism.luxspec.user.security.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.resource.UserResourceTypes;
import com.lamprism.luxspec.user.security.authorization.UserAuthorizationProfiles;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolver;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolverImpl;
import com.lamprism.luxspec.user.security.password.EncodedPassword;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import com.lamprism.luxspec.user.security.password.UserPasswordStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordAuthenticatorTest {
    private static final Instant TIME = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void authenticatesAnActiveUserAndReencodesAnOutdatedPassword() {
        User user = user(UserStatus.ACTIVE);
        RecordingPasswordStore store = new RecordingPasswordStore(new EncodedPassword("old"));
        RecordingPasswordScheme scheme = new RecordingPasswordScheme();
        PasswordAuthenticator authenticator = new PasswordAuthenticator(
                new FixedUserProvider(user),
                store,
                scheme,
                grantResolver()
        );

        Authentication authentication = authenticator.authenticate(
                new UsernamePasswordCredentials("ada", "secret")
        );

        UserSubject subject = assertInstanceOf(UserSubject.class, authentication.subject());
        assertEquals(42L, subject.userId());
        assertEquals("old", store.replacedCurrent.getValue());
        assertEquals("new", store.replacedValue.getValue());
        assertEquals(AuthorizationGrantSet.of(Set.of()), authentication.grants());
    }

    @Test
    void mapsUnknownUsersMissingPasswordsAndMismatchesToInvalidCredentials() {
        PasswordAuthenticator unknownUserAuthenticator = authenticator(
                null,
                new RecordingPasswordStore(null),
                new RecordingPasswordScheme()
        );
        assertError(AuthErrorCode.INVALID_CREDENTIALS, unknownUserAuthenticator);

        PasswordAuthenticator missingPasswordAuthenticator = authenticator(
                user(UserStatus.ACTIVE),
                new RecordingPasswordStore(null),
                new RecordingPasswordScheme()
        );
        assertError(AuthErrorCode.INVALID_CREDENTIALS, missingPasswordAuthenticator);

        PasswordAuthenticator mismatchAuthenticator = authenticator(
                user(UserStatus.ACTIVE),
                new RecordingPasswordStore(new EncodedPassword("old")),
                new RecordingPasswordScheme(false, false)
        );
        assertError(AuthErrorCode.INVALID_CREDENTIALS, mismatchAuthenticator);
    }

    @Test
    void preservesExplicitAccountStatusFailures() {
        PasswordAuthenticator authenticator = authenticator(
                user(UserStatus.DISABLED),
                new RecordingPasswordStore(new EncodedPassword("old")),
                new RecordingPasswordScheme()
        );

        assertError(AuthErrorCode.SUBJECT_DISABLED, authenticator);
    }

    @Test
    void redactsRawPasswordsFromCredentialStrings() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("ada", "secret");

        assertTrue(credentials.toString().contains("rawPassword=redacted"));
        assertFalse(credentials.toString().contains("secret"));
    }

    private static PasswordAuthenticator authenticator(
            User user,
            UserPasswordStore store,
            PasswordScheme scheme
    ) {
        return new PasswordAuthenticator(
                new FixedUserProvider(user),
                store,
                scheme,
                grantResolver()
        );
    }

    private static void assertError(AuthErrorCode expected, PasswordAuthenticator authenticator) {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticator.authenticate(new UsernamePasswordCredentials("ada", "secret"))
        );
        assertEquals(expected, exception.getErrorCode());
    }

    private static User user(UserStatus status) {
        return new User(42L, "ada", null, Set.of(Role.USER), status, TIME, TIME);
    }

    private static UserRoleGrantResolver grantResolver() {
        return new UserRoleGrantResolverImpl(
                UserAuthorizationProfiles.defaultRoleProfiles(),
                List.of(UserAuthorizationProfiles.defaults()),
                AuthorizationScopeHierarchy.empty()
        );
    }

    private static final class FixedUserProvider implements UserProvider {
        private final User user;

        private FixedUserProvider(User user) {
            this.user = user;
        }

        @Override
        public User provide(ResourceReference<Long> reference) {
            return requireUser();
        }

        @Override
        public List<User> provide(Collection<ResourceReference<Long>> references) {
            return references.stream().map(reference -> requireUser()).toList();
        }

        @Override
        public User provideByUsername(String username) {
            return requireUser();
        }

        @Override
        public ResourceType<Long> getResourceType() {
            return UserResourceTypes.USER;
        }

        private User requireUser() {
            if (user == null) {
                throw new ResourceException(CommonErrorCode.NOT_FOUND, "User was not found");
            }
            return user;
        }
    }

    private static final class RecordingPasswordStore implements UserPasswordStore {
        private final EncodedPassword currentPassword;
        private EncodedPassword replacedCurrent;
        private EncodedPassword replacedValue;

        private RecordingPasswordStore(EncodedPassword currentPassword) {
            this.currentPassword = currentPassword;
        }

        @Override
        public Optional<EncodedPassword> find(long userId) {
            return Optional.ofNullable(currentPassword);
        }

        @Override
        public boolean replace(
                long userId,
                EncodedPassword currentPassword,
                EncodedPassword replacementPassword
        ) {
            replacedCurrent = currentPassword;
            replacedValue = replacementPassword;
            return true;
        }
    }

    private static final class RecordingPasswordScheme implements PasswordScheme {
        private final boolean verifies;
        private final boolean upgrades;

        private RecordingPasswordScheme() {
            this(true, true);
        }

        private RecordingPasswordScheme(boolean verifies, boolean upgrades) {
            this.verifies = verifies;
            this.upgrades = upgrades;
        }

        @Override
        public EncodedPassword encode(CharSequence rawPassword) {
            return new EncodedPassword("new");
        }

        @Override
        public boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword) {
            return verifies;
        }

        @Override
        public boolean needsUpgrade(EncodedPassword encodedPassword) {
            return upgrades;
        }
    }
}

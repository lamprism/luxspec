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

package com.lamprism.luxspec.security.firewall.rule;

import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.firewall.AuthenticatedRequest;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.FirewallRule;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Allows or denies authenticated requests by subject type and identifier.
 *
 * @author RollW
 */
public class SubjectFirewallRule implements FirewallRule<AuthenticatedRequest> {
    private final Set<SubjectIdentity> subjects;
    private final boolean allowMatch;

    /**
     * Creates a subject rule for subclass customization.
     *
     * @param subjects   the subjects to match
     * @param allowMatch whether matching subjects pass the rule
     */
    protected SubjectFirewallRule(Collection<? extends Subject> subjects, boolean allowMatch) {
        this.subjects = copySubjects(subjects);
        this.allowMatch = allowMatch;
    }

    /**
     * Creates a subject allow-list rule.
     *
     * @param subjects the allowed authenticated subjects
     * @return the allow-list rule
     */
    public static SubjectFirewallRule allowOnly(Collection<? extends Subject> subjects) {
        return new SubjectFirewallRule(subjects, true);
    }

    /**
     * Creates a subject deny-list rule.
     *
     * @param subjects the denied authenticated subjects
     * @return the deny-list rule
     */
    public static SubjectFirewallRule deny(Collection<? extends Subject> subjects) {
        return new SubjectFirewallRule(subjects, false);
    }

    @Override
    public FirewallDecision evaluate(AuthenticatedRequest request) {
        Subject subject = Objects.requireNonNull(request, "request").getAuthentication().subject();
        boolean matched = subjects.contains(SubjectIdentity.of(subject));
        return FirewallRuleSupport.decide(
                matched,
                allowMatch,
                SecurityErrorCode.FIREWALL_SUBJECT_DENIED
        );
    }

    private static Set<SubjectIdentity> copySubjects(Collection<? extends Subject> subjects) {
        Objects.requireNonNull(subjects, "subjects");
        Set<SubjectIdentity> result = new LinkedHashSet<>();
        for (Subject subject : subjects) {
            result.add(SubjectIdentity.of(Objects.requireNonNull(subject, "subject")));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("subjects must not be empty");
        }
        if (result.size() != subjects.size()) {
            throw new IllegalArgumentException("subjects must not contain duplicates");
        }
        return Set.copyOf(result);
    }

    private static final class SubjectIdentity {
        private final String type;
        private final String id;

        private SubjectIdentity(String type, String id) {
            this.type = requireText(type, "subject type");
            this.id = requireText(id, "subject id");
        }

        private static SubjectIdentity of(Subject subject) {
            return new SubjectIdentity(subject.getType(), subject.getId());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SubjectIdentity identity)) {
                return false;
            }
            return type.equals(identity.type) && id.equals(identity.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        private static String requireText(String value, String name) {
            String nonNullValue = Objects.requireNonNull(value, name);
            if (nonNullValue.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return nonNullValue;
        }
    }
}

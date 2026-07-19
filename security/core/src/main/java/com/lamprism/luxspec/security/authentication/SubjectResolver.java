package com.lamprism.luxspec.security.authentication;

/**
 * Reconstructs and validates a subject referenced by verified token claims.
 *
 * @author RollW
 */
@FunctionalInterface
public interface SubjectResolver {
    /**
     * Resolves the current subject and applies subject-state policy for this authentication flow.
     *
     * @param subjectType the stable token subject category
     * @param subjectId the stable token subject identifier
     * @return the current subject
     * @throws AuthenticationException when the subject cannot authenticate
     */
    Subject resolve(String subjectType, String subjectId);
}

package com.lamprism.luxspec.security.authentication;


/**
 * Identifies an actor without carrying credentials or authorization grants.
 *
 * @author RollW
 */
public interface Subject {
    /**
     * Returns the stable subject category.
     *
     * @return the subject type
     */
    String getType();

    /**
     * Returns the stable subject identifier within its category.
     *
     * @return the subject identifier
     */
    String getId();
}

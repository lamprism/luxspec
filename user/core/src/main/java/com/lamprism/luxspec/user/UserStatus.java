package com.lamprism.luxspec.user;

/**
 * Describes the mutually exclusive lifecycle state of a user account.
 *
 * @author RollW
 */
public enum UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED,
    CANCELED
}

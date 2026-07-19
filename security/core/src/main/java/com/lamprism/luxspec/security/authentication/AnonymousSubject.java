package com.lamprism.luxspec.security.authentication;


/**
 * Identifies an explicit anonymous actor.
 *
 * @author RollW
 */
public final class AnonymousSubject implements Subject {
    private static final AnonymousSubject INSTANCE = new AnonymousSubject();

    private AnonymousSubject() {
    }

    /**
     * Returns the shared explicit anonymous subject.
     *
     * @return the anonymous subject
     */
    public static AnonymousSubject getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "anonymous";
    }

    @Override
    public String getId() {
        return "anonymous";
    }
}

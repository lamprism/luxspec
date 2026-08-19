package com.lamprism.luxspec.database;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a database family without coupling the core model to a driver library.
 */
public final class DatabaseType {
    public static final DatabaseType SQLITE = new DatabaseType("sqlite");
    public static final DatabaseType H2 = new DatabaseType("h2");
    public static final DatabaseType MYSQL = new DatabaseType("mysql");
    public static final DatabaseType MARIADB = new DatabaseType("mariadb");
    public static final DatabaseType POSTGRESQL = new DatabaseType("postgresql");
    public static final DatabaseType SQL_SERVER = new DatabaseType("sqlserver");
    public static final DatabaseType ORACLE = new DatabaseType("oracle");

    private final String name;

    private DatabaseType(String name) {
        this.name = name;
    }

    /**
     * Creates a database type from its stable lower-case identifier.
     *
     * @param name the database type identifier
     * @return the database type
     */
    public static DatabaseType of(String name) {
        String normalized = normalize(name);
        return new DatabaseType(normalized);
    }

    public String getName() {
        return name;
    }

    private static String normalize(String name) {
        String value = Objects.requireNonNull(name, "name").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character > 127 || !(Character.isLetterOrDigit(character))) {
                throw new IllegalArgumentException("name contains an unsupported character");
            }
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DatabaseType type && name.equals(type.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

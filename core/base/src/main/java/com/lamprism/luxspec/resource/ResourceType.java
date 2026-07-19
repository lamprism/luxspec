package com.lamprism.luxspec.resource;

import java.util.Objects;

/**
 * Identifies one resource category and its ID type.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public final class ResourceType<ID> {
    private final String name;
    private final Class<ID> idType;

    private ResourceType(String name, Class<ID> idType) {
        this.name = name;
        this.idType = idType;
    }

    /**
     * Creates a resource type with a normalized canonical name.
     *
     * @param name a camel-case, snake-case, kebab-case, or canonical type name
     * @param idType the non-primitive ID type
     * @param <ID> the resource ID type
     * @return the immutable resource type
     */
    public static <ID> ResourceType<ID> of(String name, Class<ID> idType) {
        Class<ID> nonNullIdType = Objects.requireNonNull(idType, "idType");
        if (nonNullIdType.isPrimitive()) {
            throw new IllegalArgumentException("Resource ID types must use boxed classes");
        }
        return new ResourceType<>(normalize(name), nonNullIdType);
    }

    /**
     * Returns the canonical upper-snake-case type name.
     *
     * @return the canonical type name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java type accepted as this resource category's ID.
     *
     * @return the resource ID type
     */
    public Class<ID> getIdType() {
        return idType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ResourceType<?> resourceType)) {
            return false;
        }
        return name.equals(resourceType.name) && idType.equals(resourceType.idType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, idType);
    }

    @Override
    public String toString() {
        return name;
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "name");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Resource type name must not be empty");
        }
        StringBuilder result = new StringBuilder();
        boolean previousWasSeparator = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (isAsciiLetter(character) || isDigit(character)) {
                if (index == 0 && !isAsciiLetter(character)) {
                    throw new IllegalArgumentException("Resource type name must start with a letter");
                }
                if (isUpperCase(character) && requiresWordBoundary(value, index, previousWasSeparator)) {
                    result.append('_');
                }
                result.append(toUpperCase(character));
                previousWasSeparator = false;
                continue;
            }
            if (character == '-' || character == '_') {
                if (index == 0 || previousWasSeparator || index == value.length() - 1) {
                    throw new IllegalArgumentException("Resource type name contains an empty segment");
                }
                result.append('_');
                previousWasSeparator = true;
                continue;
            }
            throw new IllegalArgumentException("Resource type name contains an unsupported character");
        }
        return result.toString();
    }

    private static boolean requiresWordBoundary(String value, int index, boolean previousWasSeparator) {
        if (previousWasSeparator || index == 0) {
            return false;
        }
        char previous = value.charAt(index - 1);
        if (isLowerCase(previous) || isDigit(previous)) {
            return true;
        }
        return isUpperCase(previous)
                && index + 1 < value.length()
                && isLowerCase(value.charAt(index + 1));
    }

    private static boolean isAsciiLetter(char character) {
        return isLowerCase(character) || isUpperCase(character);
    }

    private static boolean isLowerCase(char character) {
        return character >= 'a' && character <= 'z';
    }

    private static boolean isUpperCase(char character) {
        return character >= 'A' && character <= 'Z';
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static char toUpperCase(char character) {
        if (isLowerCase(character)) {
            return (char) (character - ('a' - 'A'));
        }
        return character;
    }
}

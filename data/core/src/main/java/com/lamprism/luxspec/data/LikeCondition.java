package com.lamprism.luxspec.data;

import java.util.Objects;

/**
 * A string pattern condition with explicit backslash escaping.
 *
 * @author RollW
 */
public final class LikeCondition implements QueryExpression {
    /**
     * Escapes a literal percent sign, underscore, or backslash in a pattern.
     */
    public static final char ESCAPE_CHARACTER = '\\';

    private final QueryField<String> field;
    private final String pattern;

    private LikeCondition(QueryField<String> field, String pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    /**
     * Creates a string pattern condition.
     *
     * @param field the string field
     * @param pattern the pattern using {@value #ESCAPE_CHARACTER} for literal wildcard characters
     * @return the immutable condition
     */
    public static LikeCondition of(QueryField<String> field, String pattern) {
        QueryField<String> nonNullField = Objects.requireNonNull(field, "field");
        String nonNullPattern = Objects.requireNonNull(pattern, "pattern");
        requireValidPattern(nonNullPattern);
        return new LikeCondition(nonNullField, nonNullPattern);
    }

    /**
     * Returns the matched string field.
     *
     * @return the string field
     */
    public QueryField<String> getField() {
        return field;
    }

    /**
     * Returns the validated pattern.
     *
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitLike(this);
    }

    private static void requireValidPattern(String pattern) {
        boolean escaped = false;
        for (int index = 0; index < pattern.length(); index++) {
            char character = pattern.charAt(index);
            if (escaped) {
                if (character != '%' && character != '_' && character != ESCAPE_CHARACTER) {
                    throw new IllegalArgumentException("LIKE escape must precede percent, underscore, or backslash");
                }
                escaped = false;
                continue;
            }
            if (character == ESCAPE_CHARACTER) {
                escaped = true;
            }
        }
        if (escaped) {
            throw new IllegalArgumentException("LIKE pattern must not end with an escape character");
        }
    }
}

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.naming.CaseFormat;
import com.lamprism.luxspec.naming.NameConverter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Converts one configuration value to and from its provider-neutral raw representation.
 *
 * <p>A codec owns representation conversion, not the definition's business constraints. A codec
 * may accept more than one raw scalar kind when those representations have the same meaning for
 * its type. For example, a size codec can accept an integral raw value as bytes and a string raw
 * value such as {@code "1KB"}. This policy is independent of whether the source is TOML, a
 * database, or an environment adapter.</p>
 *
 * @param <T> the typed value
 * @author RollW
 */
public interface ConfigCodec<T> {
    /**
     * Returns a codec for required string values.
     *
     * @return the string codec
     */
    static ConfigCodec<String> string() {
        return ConfigCodecFactory.string();
    }

    /**
     * Returns a codec for Boolean values.
     *
     * @return the Boolean codec
     */
    static ConfigCodec<Boolean> booleanValue() {
        return ConfigCodecFactory.booleanValue();
    }

    /**
     * Returns a codec for 32-bit integer values.
     *
     * @return the integer codec
     */
    static ConfigCodec<Integer> integer() {
        return ConfigCodecFactory.integer();
    }

    /**
     * Returns a codec for 64-bit integer values.
     *
     * @return the long codec
     */
    static ConfigCodec<Long> longValue() {
        return ConfigCodecFactory.longValue();
    }

    /**
     * Returns a codec for decimal values.
     *
     * @return the decimal codec
     */
    static ConfigCodec<BigDecimal> decimal() {
        return ConfigCodecFactory.decimal();
    }

    /**
     * Returns a codec for instant values.
     *
     * @return the instant codec
     */
    static ConfigCodec<Instant> instant() {
        return ConfigCodecFactory.instant();
    }

    /**
     * Returns a codec for local dates.
     *
     * @return the local date codec
     */
    static ConfigCodec<LocalDate> localDate() {
        return ConfigCodecFactory.localDate();
    }

    /**
     * Returns a codec for local times.
     *
     * @return the local time codec
     */
    static ConfigCodec<LocalTime> localTime() {
        return ConfigCodecFactory.localTime();
    }

    /**
     * Returns a codec for local date-time values.
     *
     * @return the local date-time codec
     */
    static ConfigCodec<LocalDateTime> localDateTime() {
        return ConfigCodecFactory.localDateTime();
    }

    /**
     * Returns a codec for offset times.
     *
     * @return the offset time codec
     */
    static ConfigCodec<OffsetTime> offsetTime() {
        return ConfigCodecFactory.offsetTime();
    }

    /**
     * Returns a codec for offset date-time values.
     *
     * @return the offset date-time codec
     */
    static ConfigCodec<OffsetDateTime> offsetDateTime() {
        return ConfigCodecFactory.offsetDateTime();
    }

    /**
     * Returns a codec for zoned date-time values.
     *
     * @return the zoned date-time codec
     */
    static ConfigCodec<ZonedDateTime> zonedDateTime() {
        return ConfigCodecFactory.zonedDateTime();
    }

    /**
     * Returns a codec for year values.
     *
     * @return the year codec
     */
    static ConfigCodec<Year> year() {
        return ConfigCodecFactory.year();
    }

    /**
     * Returns a codec for year-month values.
     *
     * @return the year-month codec
     */
    static ConfigCodec<YearMonth> yearMonth() {
        return ConfigCodecFactory.yearMonth();
    }

    /**
     * Returns a codec for month-day values.
     *
     * @return the month-day codec
     */
    static ConfigCodec<MonthDay> monthDay() {
        return ConfigCodecFactory.monthDay();
    }

    /**
     * Returns a codec for zone IDs.
     *
     * @return the zone ID codec
     */
    static ConfigCodec<ZoneId> zoneId() {
        return ConfigCodecFactory.zoneId();
    }

    /**
     * Returns a codec for ISO-8601 and short-unit durations.
     *
     * @return the duration codec
     */
    static ConfigCodec<Duration> duration() {
        return ConfigCodecFactory.duration();
    }

    /**
     * Returns a codec for period values.
     *
     * @return the period codec
     */
    static ConfigCodec<Period> period() {
        return ConfigCodecFactory.period();
    }

    /**
     * Returns an enum codec using lower-hyphen external names.
     *
     * @param enumType the enum type
     * @param <E>      the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(Class<E> enumType) {
        return ConfigCodecFactory.enumValue(enumType);
    }

    /**
     * Returns an enum codec using lower-hyphen external names.
     *
     * @param enumValues the enum constants
     * @param <E>        the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(E[] enumValues) {
        return ConfigCodecFactory.enumValue(enumValues);
    }

    /**
     * Returns an enum codec with an explicit external case format.
     *
     * @param enumType     the enum type
     * @param targetFormat the external case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            CaseFormat targetFormat
    ) {
        return ConfigCodecFactory.enumValue(enumType, targetFormat);
    }

    /**
     * Returns an enum codec with an explicit external case format.
     *
     * @param enumValues   the enum constants
     * @param targetFormat the external case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            CaseFormat targetFormat
    ) {
        return ConfigCodecFactory.enumValue(enumValues, targetFormat);
    }

    /**
     * Returns an enum codec with explicit source and external case formats.
     *
     * @param enumType     the enum type
     * @param sourceFormat the source enum-name format
     * @param targetFormat the external case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            CaseFormat sourceFormat,
            CaseFormat targetFormat
    ) {
        return ConfigCodecFactory.enumValue(enumType, sourceFormat, targetFormat);
    }

    /**
     * Returns an enum codec with explicit source and external case formats.
     *
     * @param enumValues   the enum constants
     * @param sourceFormat the source enum-name format
     * @param targetFormat the external case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            CaseFormat sourceFormat,
            CaseFormat targetFormat
    ) {
        return ConfigCodecFactory.enumValue(enumValues, sourceFormat, targetFormat);
    }

    /**
     * Returns an enum codec with an application-defined name converter.
     *
     * @param enumType      the enum type
     * @param nameConverter converts enum names to configuration values
     * @param <E>           the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            NameConverter nameConverter
    ) {
        return ConfigCodecFactory.enumValue(enumType, nameConverter);
    }

    /**
     * Returns an enum codec with an application-defined name converter.
     *
     * @param enumValues    the enum constants
     * @param nameConverter converts enum names to configuration values
     * @param <E>           the enum type
     * @return the enum codec
     */
    static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            NameConverter nameConverter
    ) {
        return ConfigCodecFactory.enumValue(enumValues, nameConverter);
    }

    /**
     * Creates a codec for a type represented by one textual serialized value.
     *
     * @param deserializer converts raw text to the typed value
     * @param serializer   converts the typed value to raw text
     * @param <T>          the typed value
     * @return the serialized-value codec
     */
    static <T> ConfigCodec<T> serialized(
            Function<String, T> deserializer,
            Function<T, String> serializer
    ) {
        return ConfigCodecFactory.serialized(deserializer, serializer);
    }

    /**
     * Creates a codec for a list of values.
     *
     * @param elementCodec the codec for each element
     * @param <T>          the element type
     * @return the list codec
     */
    static <T> ConfigCodec<List<T>> list(ConfigCodec<T> elementCodec) {
        return ConfigCodecFactory.list(elementCodec);
    }

    /**
     * Decodes a raw source value.
     *
     * @param rawValue the scalar or list source value
     * @return the decoded value
     * @throws IllegalArgumentException when the raw value is invalid for this codec
     */
    T decode(RawConfigValue rawValue);

    /**
     * Encodes a typed value without losing scalar or list boundaries.
     *
     * @param value the typed value
     * @return the provider-neutral raw value
     * @throws IllegalArgumentException when the typed value cannot be encoded
     */
    RawConfigValue encode(T value);
}

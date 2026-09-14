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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Provides the implementation behind the singular ConfigCodec factory methods.
 *
 * @author RollW
 */
final class ConfigCodecFactory {
    private static final ConfigCodec<String> STRING = scalar(
            RawConfigValue::requireString,
            RawConfigValue::string
    );
    private static final ConfigCodec<Boolean> BOOLEAN = new BooleanCodec();
    private static final ConfigCodec<Integer> INTEGER = scalar(
            ConfigCodecFactory::decodeInteger,
            value -> RawConfigValue.integer(Objects.requireNonNull(value, "value"))
    );
    private static final ConfigCodec<Long> LONG = scalar(
            ConfigCodecFactory::decodeLong,
            value -> RawConfigValue.integer(Objects.requireNonNull(value, "value"))
    );
    private static final ConfigCodec<BigDecimal> DECIMAL = scalar(
            ConfigCodecFactory::decodeDecimal,
            value -> RawConfigValue.decimal(Objects.requireNonNull(value, "value"))
    );
    private static final ConfigCodec<Instant> INSTANT = text(Instant::parse, Instant::toString);
    private static final ConfigCodec<LocalDate> LOCAL_DATE = text(LocalDate::parse, LocalDate::toString);
    private static final ConfigCodec<LocalTime> LOCAL_TIME = text(LocalTime::parse, LocalTime::toString);
    private static final ConfigCodec<LocalDateTime> LOCAL_DATE_TIME = text(
            LocalDateTime::parse,
            LocalDateTime::toString
    );
    private static final ConfigCodec<OffsetTime> OFFSET_TIME = text(OffsetTime::parse, OffsetTime::toString);
    private static final ConfigCodec<OffsetDateTime> OFFSET_DATE_TIME = text(
            OffsetDateTime::parse,
            OffsetDateTime::toString
    );
    private static final ConfigCodec<ZonedDateTime> ZONED_DATE_TIME = text(
            ZonedDateTime::parse,
            ZonedDateTime::toString
    );
    private static final ConfigCodec<Year> YEAR = text(Year::parse, Year::toString);
    private static final ConfigCodec<YearMonth> YEAR_MONTH = text(YearMonth::parse, YearMonth::toString);
    private static final ConfigCodec<MonthDay> MONTH_DAY = text(MonthDay::parse, MonthDay::toString);
    private static final ConfigCodec<ZoneId> ZONE_ID = text(ZoneId::of, ZoneId::getId);
    private static final ConfigCodec<Duration> DURATION = new DurationCodec();
    private static final ConfigCodec<Period> PERIOD = text(Period::parse, Period::toString);
    private static final NameConverter DEFAULT_ENUM_VALUE_CONVERTER =
            CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN);

    private ConfigCodecFactory() {
    }

    /**
     * Returns a codec for required string values.
     *
     * @return the string codec
     */
    public static ConfigCodec<String> string() {
        return STRING;
    }

    /**
     * Returns a codec for Boolean values.
     *
     * @return the Boolean codec
     */
    public static ConfigCodec<Boolean> booleanValue() {
        return BOOLEAN;
    }

    /**
     * Returns a codec for 32-bit integer values.
     *
     * @return the integer codec
     */
    public static ConfigCodec<Integer> integer() {
        return INTEGER;
    }

    /**
     * Returns a codec for 64-bit integer values.
     *
     * @return the long codec
     */
    public static ConfigCodec<Long> longValue() {
        return LONG;
    }

    /**
     * Returns a codec for decimal values.
     *
     * @return the decimal codec
     */
    public static ConfigCodec<BigDecimal> decimal() {
        return DECIMAL;
    }

    /**
     * Returns a codec for instant values.
     *
     * @return the instant codec
     */
    public static ConfigCodec<Instant> instant() {
        return INSTANT;
    }

    /**
     * Returns a codec for local dates.
     *
     * @return the local date codec
     */
    public static ConfigCodec<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    /**
     * Returns a codec for local times.
     *
     * @return the local time codec
     */
    public static ConfigCodec<LocalTime> localTime() {
        return LOCAL_TIME;
    }

    /**
     * Returns a codec for local date-time values.
     *
     * @return the local date-time codec
     */
    public static ConfigCodec<LocalDateTime> localDateTime() {
        return LOCAL_DATE_TIME;
    }

    /**
     * Returns a codec for offset times.
     *
     * @return the offset time codec
     */
    public static ConfigCodec<OffsetTime> offsetTime() {
        return OFFSET_TIME;
    }

    /**
     * Returns a codec for offset date-time values.
     *
     * @return the offset date-time codec
     */
    public static ConfigCodec<OffsetDateTime> offsetDateTime() {
        return OFFSET_DATE_TIME;
    }

    /**
     * Returns a codec for zoned date-time values.
     *
     * @return the zoned date-time codec
     */
    public static ConfigCodec<ZonedDateTime> zonedDateTime() {
        return ZONED_DATE_TIME;
    }

    /**
     * Returns a codec for year values.
     *
     * @return the year codec
     */
    public static ConfigCodec<Year> year() {
        return YEAR;
    }

    /**
     * Returns a codec for year-month values.
     *
     * @return the year-month codec
     */
    public static ConfigCodec<YearMonth> yearMonth() {
        return YEAR_MONTH;
    }

    /**
     * Returns a codec for month-day values.
     *
     * @return the month-day codec
     */
    public static ConfigCodec<MonthDay> monthDay() {
        return MONTH_DAY;
    }

    /**
     * Returns a codec for zone IDs.
     *
     * @return the zone ID codec
     */
    public static ConfigCodec<ZoneId> zoneId() {
        return ZONE_ID;
    }

    /**
     * Returns a codec for ISO-8601 and short-unit durations.
     *
     * @return the duration codec
     */
    public static ConfigCodec<Duration> duration() {
        return DURATION;
    }

    /**
     * Returns a codec for period values.
     *
     * @return the period codec
     */
    public static ConfigCodec<Period> period() {
        return PERIOD;
    }

    /**
     * Returns an enum codec that stores Java enum names as lower-hyphen values.
     *
     * <p>For example, {@code READ_ONLY} is encoded as {@code "read-only"}. The default is based
     * on the conventional Java enum name format {@link CaseFormat#UPPER_UNDERSCORE}.</p>
     *
     * @param enumType the enum type
     * @param <E>      the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(Class<E> enumType) {
        return enumValue(enumType, DEFAULT_ENUM_VALUE_CONVERTER);
    }

    /**
     * Returns a native-friendly enum codec that stores Java enum names as lower-hyphen values.
     *
     * <p>The constants are supplied by the caller, so creating this codec does not need to
     * inspect the enum type at runtime.</p>
     *
     * @param enumValues the enum constants
     * @param <E>        the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(E[] enumValues) {
        return enumValue(enumValues, DEFAULT_ENUM_VALUE_CONVERTER);
    }

    /**
     * Returns an enum codec that converts conventional Java enum names to the target case format.
     *
     * @param enumType     the enum type
     * @param targetFormat the external configuration case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            CaseFormat targetFormat
    ) {
        return enumValue(enumType, CaseFormat.UPPER_UNDERSCORE, targetFormat);
    }

    /**
     * Returns a native-friendly enum codec with an explicit external case format.
     *
     * @param enumValues   the enum constants
     * @param targetFormat the external configuration case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            CaseFormat targetFormat
    ) {
        return enumValue(enumValues, CaseFormat.UPPER_UNDERSCORE, targetFormat);
    }

    /**
     * Returns an enum codec with explicit source and external case formats.
     *
     * @param enumType     the enum type
     * @param sourceFormat the format used by enum constant names
     * @param targetFormat the external configuration case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            CaseFormat sourceFormat,
            CaseFormat targetFormat
    ) {
        CaseFormat nonNullSourceFormat = Objects.requireNonNull(sourceFormat, "sourceFormat");
        CaseFormat nonNullTargetFormat = Objects.requireNonNull(targetFormat, "targetFormat");
        return enumValue(enumType, nonNullSourceFormat.to(nonNullTargetFormat));
    }

    /**
     * Returns a native-friendly enum codec with explicit source and external case formats.
     *
     * @param enumValues   the enum constants
     * @param sourceFormat the format used by enum constant names
     * @param targetFormat the external configuration case format
     * @param <E>          the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            CaseFormat sourceFormat,
            CaseFormat targetFormat
    ) {
        CaseFormat nonNullSourceFormat = Objects.requireNonNull(sourceFormat, "sourceFormat");
        CaseFormat nonNullTargetFormat = Objects.requireNonNull(targetFormat, "targetFormat");
        return enumValue(enumValues, nonNullSourceFormat.to(nonNullTargetFormat));
    }

    /**
     * Returns an enum codec with an application-defined external name conversion.
     *
     * <p>The converter is applied to each enum constant name when the codec is created. Decoding
     * uses the resulting names directly, so the converter does not need to be reversible.</p>
     *
     * @param enumType      the enum type
     * @param nameConverter converts enum constant names to configuration values
     * @param <E>           the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            Class<E> enumType,
            NameConverter nameConverter
    ) {
        Class<E> nonNullEnumType = Objects.requireNonNull(enumType, "enumType");
        E[] enumValues = nonNullEnumType.getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("Type must be an enum");
        }
        return enumValue(enumValues, nameConverter);
    }

    /**
     * Returns a native-friendly enum codec with an application-defined external name conversion.
     *
     * @param enumValues    the enum constants
     * @param nameConverter converts enum constant names to configuration values
     * @param <E>           the enum type
     * @return the enum codec
     */
    public static <E extends Enum<E>> ConfigCodec<E> enumValue(
            E[] enumValues,
            NameConverter nameConverter
    ) {
        return new EnumCodec<>(enumValues, nameConverter);
    }

    /**
     * Creates a codec for a type represented by one textual serialized value.
     *
     * <p>The format is intentionally owned by the supplied functions. This factory can therefore
     * be used with JSON, CBOR text, URI values, or an application-specific representation without
     * adding a serialization library to Config Core.</p>
     *
     * @param deserializer converts the raw text to the typed value
     * @param serializer   converts the typed value to raw text
     * @param <T>          the typed value
     * @return the serialized-value codec
     */
    public static <T> ConfigCodec<T> serialized(
            Function<String, T> deserializer,
            Function<T, String> serializer
    ) {
        return text(deserializer, serializer);
    }

    /**
     * Creates a list codec that preserves each raw element's kind and boundary.
     *
     * @param elementCodec the codec for each element
     * @param <T>          the element type
     * @return the list codec
     */
    public static <T> ConfigCodec<List<T>> list(ConfigCodec<T> elementCodec) {
        return new ListCodec<>(elementCodec);
    }

    private static <T> ConfigCodec<T> scalar(
            Function<RawConfigValue, T> decoder,
            Function<T, RawConfigValue> encoder
    ) {
        return new ScalarCodec<>(decoder, encoder);
    }

    private static <T> ConfigCodec<T> text(
            Function<String, T> decoder,
            Function<T, String> encoder
    ) {
        Function<String, T> nonNullDecoder = Objects.requireNonNull(decoder, "decoder");
        Function<T, String> nonNullEncoder = Objects.requireNonNull(encoder, "encoder");
        return scalar(
                raw -> nonNullDecoder.apply(raw.requireText()),
                value -> RawConfigValue.string(nonNullEncoder.apply(value))
        );
    }

    private static Integer decodeInteger(RawConfigValue rawValue) {
        return switch (rawValue.getScalarKind()) {
            case STRING -> Integer.valueOf(rawValue.requireString());
            case INTEGER -> Math.toIntExact(rawValue.requireInteger());
            case DECIMAL -> throw new IllegalArgumentException("Decimal value is not an integer");
            case BOOLEAN -> throw new IllegalArgumentException("Boolean value is not an integer");
        };
    }

    private static Long decodeLong(RawConfigValue rawValue) {
        return switch (rawValue.getScalarKind()) {
            case STRING -> Long.valueOf(rawValue.requireString());
            case INTEGER -> rawValue.requireInteger();
            case DECIMAL -> throw new IllegalArgumentException("Decimal value is not an integer");
            case BOOLEAN -> throw new IllegalArgumentException("Boolean value is not an integer");
        };
    }

    private static BigDecimal decodeDecimal(RawConfigValue rawValue) {
        return switch (rawValue.getScalarKind()) {
            case STRING -> new BigDecimal(rawValue.requireString());
            case INTEGER -> BigDecimal.valueOf(rawValue.requireInteger());
            case DECIMAL -> rawValue.requireDecimal();
            case BOOLEAN -> throw new IllegalArgumentException("Boolean value is not a decimal");
        };
    }

    private static final class ScalarCodec<T> implements ConfigCodec<T> {
        private final Function<RawConfigValue, T> decoder;
        private final Function<T, RawConfigValue> encoder;

        private ScalarCodec(
                Function<RawConfigValue, T> decoder,
                Function<T, RawConfigValue> encoder
        ) {
            this.decoder = Objects.requireNonNull(decoder, "decoder");
            this.encoder = Objects.requireNonNull(encoder, "encoder");
        }

        @Override
        public T decode(RawConfigValue rawValue) {
            return Objects.requireNonNull(
                    decoder.apply(Objects.requireNonNull(rawValue, "rawValue")),
                    "decoded value"
            );
        }

        @Override
        public RawConfigValue encode(T value) {
            return Objects.requireNonNull(
                    encoder.apply(Objects.requireNonNull(value, "value")),
                    "encoded value"
            );
        }
    }

    private static final class EnumCodec<E extends Enum<E>> implements ConfigCodec<E> {
        private final Class<?> enumType;
        private final NameConverter nameConverter;
        private final Map<String, E> valuesByName;

        private EnumCodec(E[] enumValues, NameConverter nameConverter) {
            E[] nonNullEnumValues = requireEnumValues(enumValues);
            this.enumType = nonNullEnumValues[0].getDeclaringClass();
            this.nameConverter = Objects.requireNonNull(nameConverter, "nameConverter");
            this.valuesByName = createValuesByName(nonNullEnumValues, this.nameConverter);
        }

        @Override
        public E decode(RawConfigValue rawValue) {
            String serializedValue = Objects.requireNonNull(rawValue, "rawValue").requireText();
            E value = valuesByName.get(serializedValue);
            if (value == null) {
                throw new IllegalArgumentException("Enum value is unsupported for " + enumType.getName());
            }
            return value;
        }

        @Override
        public RawConfigValue encode(E value) {
            E nonNullValue = Objects.requireNonNull(value, "value");
            String serializedName = Objects.requireNonNull(
                    nameConverter.convert(nonNullValue.name()),
                    "converted enum name"
            );
            return RawConfigValue.string(serializedName);
        }

        private static <E extends Enum<E>> E[] requireEnumValues(E[] enumValues) {
            E[] nonNullEnumValues = Objects.requireNonNull(enumValues, "enumValues").clone();
            if (nonNullEnumValues.length == 0) {
                throw new IllegalArgumentException("Enum values must not be empty");
            }
            Class<?> enumType = nonNullEnumValues[0].getDeclaringClass();
            for (int index = 1; index < nonNullEnumValues.length; index++) {
                E nonNullEnumValue = Objects.requireNonNull(nonNullEnumValues[index], "enum value");
                if (enumType != nonNullEnumValue.getDeclaringClass()) {
                    throw new IllegalArgumentException("Enum values must have the same type");
                }
            }
            return nonNullEnumValues;
        }

        private static <E extends Enum<E>> Map<String, E> createValuesByName(
                E[] enumValues,
                NameConverter nameConverter
        ) {
            Map<String, E> values = new LinkedHashMap<>();
            for (E enumValue : enumValues) {
                String serializedName = Objects.requireNonNull(
                        nameConverter.convert(enumValue.name()),
                        "converted enum name"
                );
                if (serializedName.isEmpty()) {
                    throw new IllegalArgumentException("Converted enum name must not be empty");
                }
                E previous = values.put(serializedName, enumValue);
                if (previous != null) {
                    throw new IllegalArgumentException("Enum names must be unique after conversion");
                }
            }
            return Map.copyOf(values);
        }
    }

    private static final class BooleanCodec implements ConfigCodec<Boolean> {
        @Override
        public Boolean decode(RawConfigValue rawValue) {
            RawConfigValue nonNullRawValue = Objects.requireNonNull(rawValue, "rawValue");
            return switch (nonNullRawValue.getScalarKind()) {
                case BOOLEAN -> nonNullRawValue.requireBoolean();
                case STRING -> parseBoolean(nonNullRawValue.requireString());
                case INTEGER, DECIMAL -> throw new IllegalArgumentException("Numeric value is not Boolean");
            };
        }

        @Override
        public RawConfigValue encode(Boolean value) {
            return RawConfigValue.booleanValue(Objects.requireNonNull(value, "value"));
        }

        private static boolean parseBoolean(String value) {
            if ("true".equals(value)) {
                return true;
            }
            if ("false".equals(value)) {
                return false;
            }
            throw new IllegalArgumentException("Boolean value must be true or false");
        }
    }

    private static final class DurationCodec implements ConfigCodec<Duration> {
        @Override
        public Duration decode(RawConfigValue rawValue) {
            String value = Objects.requireNonNull(rawValue, "rawValue").requireText();
            if (value.startsWith("P") || value.startsWith("-P")) {
                return Duration.parse(value);
            }
            return parseShortDuration(value);
        }

        @Override
        public RawConfigValue encode(Duration value) {
            return RawConfigValue.string(Objects.requireNonNull(value, "value").toString());
        }

        private Duration parseShortDuration(String value) {
            int unitStart = 0;
            while (unitStart < value.length() && Character.isDigit(value.charAt(unitStart))) {
                unitStart++;
            }
            if (unitStart == 0 || unitStart == value.length()) {
                throw new IllegalArgumentException("Duration must use an ISO-8601 or short-unit representation");
            }
            long amount = Long.parseLong(value.substring(0, unitStart));
            String unit = value.substring(unitStart);
            return switch (unit) {
                case "ms" -> Duration.ofMillis(amount);
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> throw new IllegalArgumentException("Duration unit is unsupported");
            };
        }
    }

    private static final class ListCodec<T> implements ConfigCodec<List<T>> {
        private final ConfigCodec<T> elementCodec;

        private ListCodec(ConfigCodec<T> elementCodec) {
            this.elementCodec = Objects.requireNonNull(elementCodec, "elementCodec");
        }

        @Override
        public List<T> decode(RawConfigValue rawValue) {
            List<T> values = new ArrayList<>();
            for (RawConfigValue element : Objects.requireNonNull(rawValue, "rawValue").requireList()) {
                values.add(elementCodec.decode(element));
            }
            return List.copyOf(values);
        }

        @Override
        public RawConfigValue encode(List<T> value) {
            List<RawConfigValue> rawValues = new ArrayList<>();
            for (T element : Objects.requireNonNull(value, "value")) {
                rawValues.add(elementCodec.encode(element));
            }
            return RawConfigValue.list(rawValues);
        }
    }
}

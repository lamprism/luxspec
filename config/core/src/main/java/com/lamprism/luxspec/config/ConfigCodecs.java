package com.lamprism.luxspec.config;

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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Provides strict codecs for common scalar, time, and list configuration values.
 *
 * @author RollW
 */
public final class ConfigCodecs {
    private static final ConfigCodec<String> STRING = scalar(Function.identity(), Function.identity());
    private static final ConfigCodec<Boolean> BOOLEAN = new BooleanCodec();
    private static final ConfigCodec<Integer> INTEGER = scalar(Integer::valueOf, Object::toString);
    private static final ConfigCodec<Long> LONG = scalar(Long::valueOf, Object::toString);
    private static final ConfigCodec<BigDecimal> DECIMAL = scalar(BigDecimal::new, BigDecimal::toPlainString);
    private static final ConfigCodec<Instant> INSTANT = scalar(Instant::parse, Instant::toString);
    private static final ConfigCodec<LocalDate> LOCAL_DATE = scalar(LocalDate::parse, LocalDate::toString);
    private static final ConfigCodec<LocalTime> LOCAL_TIME = scalar(LocalTime::parse, LocalTime::toString);
    private static final ConfigCodec<LocalDateTime> LOCAL_DATE_TIME = scalar(LocalDateTime::parse, LocalDateTime::toString);
    private static final ConfigCodec<OffsetTime> OFFSET_TIME = scalar(OffsetTime::parse, OffsetTime::toString);
    private static final ConfigCodec<OffsetDateTime> OFFSET_DATE_TIME = scalar(OffsetDateTime::parse, OffsetDateTime::toString);
    private static final ConfigCodec<ZonedDateTime> ZONED_DATE_TIME = scalar(ZonedDateTime::parse, ZonedDateTime::toString);
    private static final ConfigCodec<Year> YEAR = scalar(Year::parse, Year::toString);
    private static final ConfigCodec<YearMonth> YEAR_MONTH = scalar(YearMonth::parse, YearMonth::toString);
    private static final ConfigCodec<MonthDay> MONTH_DAY = scalar(MonthDay::parse, MonthDay::toString);
    private static final ConfigCodec<ZoneId> ZONE_ID = scalar(ZoneId::of, ZoneId::getId);
    private static final ConfigCodec<Duration> DURATION = new DurationCodec();
    private static final ConfigCodec<Period> PERIOD = scalar(Period::parse, Period::toString);

    private ConfigCodecs() {
    }

    /**
     * Returns the scalar String codec.
     *
     * @return the String codec
     */
    public static ConfigCodec<String> string() {
        return STRING;
    }

    /**
     * Returns the strict lowercase Boolean codec.
     *
     * @return the Boolean codec
     */
    public static ConfigCodec<Boolean> booleanValue() {
        return BOOLEAN;
    }

    /**
     * Returns the base-10 Integer codec.
     *
     * @return the Integer codec
     */
    public static ConfigCodec<Integer> integer() {
        return INTEGER;
    }

    /**
     * Returns the base-10 Long codec.
     *
     * @return the Long codec
     */
    public static ConfigCodec<Long> longValue() {
        return LONG;
    }

    /**
     * Returns the plain-decimal BigDecimal codec.
     *
     * @return the BigDecimal codec
     */
    public static ConfigCodec<BigDecimal> decimal() {
        return DECIMAL;
    }

    /**
     * Returns the ISO-8601 Instant codec.
     *
     * @return the Instant codec
     */
    public static ConfigCodec<Instant> instant() {
        return INSTANT;
    }

    /**
     * Returns the ISO-8601 LocalDate codec.
     *
     * @return the LocalDate codec
     */
    public static ConfigCodec<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    /**
     * Returns the ISO-8601 LocalTime codec.
     *
     * @return the LocalTime codec
     */
    public static ConfigCodec<LocalTime> localTime() {
        return LOCAL_TIME;
    }

    /**
     * Returns the ISO-8601 LocalDateTime codec.
     *
     * @return the LocalDateTime codec
     */
    public static ConfigCodec<LocalDateTime> localDateTime() {
        return LOCAL_DATE_TIME;
    }

    /**
     * Returns the ISO-8601 OffsetTime codec.
     *
     * @return the OffsetTime codec
     */
    public static ConfigCodec<OffsetTime> offsetTime() {
        return OFFSET_TIME;
    }

    /**
     * Returns the ISO-8601 OffsetDateTime codec.
     *
     * @return the OffsetDateTime codec
     */
    public static ConfigCodec<OffsetDateTime> offsetDateTime() {
        return OFFSET_DATE_TIME;
    }

    /**
     * Returns the ISO-8601 ZonedDateTime codec.
     *
     * @return the ZonedDateTime codec
     */
    public static ConfigCodec<ZonedDateTime> zonedDateTime() {
        return ZONED_DATE_TIME;
    }

    /**
     * Returns the ISO-8601 Year codec.
     *
     * @return the Year codec
     */
    public static ConfigCodec<Year> year() {
        return YEAR;
    }

    /**
     * Returns the ISO-8601 YearMonth codec.
     *
     * @return the YearMonth codec
     */
    public static ConfigCodec<YearMonth> yearMonth() {
        return YEAR_MONTH;
    }

    /**
     * Returns the ISO-8601 MonthDay codec.
     *
     * @return the MonthDay codec
     */
    public static ConfigCodec<MonthDay> monthDay() {
        return MONTH_DAY;
    }

    /**
     * Returns the ZoneId codec.
     *
     * @return the ZoneId codec
     */
    public static ConfigCodec<ZoneId> zoneId() {
        return ZONE_ID;
    }

    /**
     * Returns the strict ISO-8601 and short-unit Duration codec.
     *
     * @return the Duration codec
     */
    public static ConfigCodec<Duration> duration() {
        return DURATION;
    }

    /**
     * Returns the ISO-8601 Period codec.
     *
     * @return the Period codec
     */
    public static ConfigCodec<Period> period() {
        return PERIOD;
    }

    /**
     * Creates a list codec that preserves one raw element for each typed element.
     *
     * @param elementCodec the scalar codec for each element
     * @param <T> the element type
     * @return the list codec
     */
    public static <T> ConfigCodec<List<T>> list(ConfigCodec<T> elementCodec) {
        return new ListCodec<>(elementCodec);
    }

    private static <T> ConfigCodec<T> scalar(Function<String, T> decoder, Function<T, String> encoder) {
        return new ScalarCodec<>(decoder, encoder);
    }

    private static final class ScalarCodec<T> implements ConfigCodec<T> {
        private final Function<String, T> decoder;
        private final Function<T, String> encoder;

        private ScalarCodec(Function<String, T> decoder, Function<T, String> encoder) {
            this.decoder = Objects.requireNonNull(decoder, "decoder");
            this.encoder = Objects.requireNonNull(encoder, "encoder");
        }

        @Override
        public T decode(RawConfigValue rawValue) {
            return decoder.apply(Objects.requireNonNull(rawValue, "rawValue").requireScalar());
        }

        @Override
        public RawConfigValue encode(T value) {
            return RawConfigValue.scalar(encoder.apply(Objects.requireNonNull(value, "value")));
        }
    }

    private static final class BooleanCodec implements ConfigCodec<Boolean> {
        @Override
        public Boolean decode(RawConfigValue rawValue) {
            String value = Objects.requireNonNull(rawValue, "rawValue").requireScalar();
            if ("true".equals(value)) {
                return true;
            }
            if ("false".equals(value)) {
                return false;
            }
            throw new IllegalArgumentException("Boolean value must be true or false");
        }

        @Override
        public RawConfigValue encode(Boolean value) {
            return RawConfigValue.scalar(Objects.requireNonNull(value, "value").toString());
        }
    }

    private static final class DurationCodec implements ConfigCodec<Duration> {
        @Override
        public Duration decode(RawConfigValue rawValue) {
            String value = Objects.requireNonNull(rawValue, "rawValue").requireScalar();
            if (value.startsWith("P") || value.startsWith("-P")) {
                return Duration.parse(value);
            }
            return parseShortDuration(value);
        }

        @Override
        public RawConfigValue encode(Duration value) {
            return RawConfigValue.scalar(Objects.requireNonNull(value, "value").toString());
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
            for (String element : Objects.requireNonNull(rawValue, "rawValue").requireList()) {
                values.add(elementCodec.decode(RawConfigValue.scalar(element)));
            }
            return List.copyOf(values);
        }

        @Override
        public RawConfigValue encode(List<T> value) {
            List<String> rawValues = new ArrayList<>();
            for (T element : Objects.requireNonNull(value, "value")) {
                rawValues.add(elementCodec.encode(element).requireScalar());
            }
            return RawConfigValue.list(rawValues);
        }
    }
}

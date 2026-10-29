/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.data.query;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Defines fixed-width time buckets anchored at an explicit instant.
 *
 * <p>The implementation uses nanosecond arithmetic with arbitrary precision so negative
 * timestamps and long ranges use the same floor-bucket rule without overflowing a {@code long}.
 * The returned bucket timestamp is always the inclusive bucket start.</p>
 *
 * @author RollW
 */
public final class FixedTimeSeriesBucket implements TimeSeriesBucket {
    private static final BigInteger NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger ONE = BigInteger.ONE;

    private final Duration size;
    private final Instant origin;
    private final BigInteger sizeNanos;

    FixedTimeSeriesBucket(Duration size, Instant origin) {
        Duration nonNullSize = Objects.requireNonNull(size, "size");
        if (nonNullSize.isZero() || nonNullSize.isNegative()) {
            throw new IllegalArgumentException("Bucket size must be positive");
        }
        this.size = nonNullSize;
        this.origin = Objects.requireNonNull(origin, "origin");
        this.sizeNanos = toNanos(nonNullSize);
    }

    /**
     * Returns the positive bucket width.
     *
     * @return the bucket width
     */
    public Duration size() {
        return size;
    }

    /**
     * Returns the bucket origin.
     *
     * @return the bucket origin
     */
    public Instant origin() {
        return origin;
    }

    /**
     * Returns the inclusive start of the bucket containing a timestamp.
     *
     * @param timestamp the timestamp to bucket
     * @return the bucket start
     */
    @Override
    public Instant start(Instant timestamp) {
        Instant nonNullTimestamp = Objects.requireNonNull(timestamp, "timestamp");
        BigInteger delta = nanosBetween(origin, nonNullTimestamp);
        BigInteger[] division = delta.divideAndRemainder(sizeNanos);
        BigInteger index = division[0];
        if (delta.signum() < 0 && division[1].signum() != 0) {
            index = index.subtract(ONE);
        }
        BigInteger bucketDelta = sizeNanos.multiply(index);
        BigInteger[] secondsAndNanos = bucketDelta.divideAndRemainder(NANOS_PER_SECOND);
        BigInteger relativeSeconds = secondsAndNanos[0];
        BigInteger relativeNanos = secondsAndNanos[1];
        if (relativeNanos.signum() < 0) {
            relativeSeconds = relativeSeconds.subtract(ONE);
            relativeNanos = relativeNanos.add(NANOS_PER_SECOND);
        }
        try {
            return origin.plusSeconds(relativeSeconds.longValueExact())
                    .plusNanos(relativeNanos.longValueExact());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Bucket start exceeds the Instant range", exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FixedTimeSeriesBucket bucket)) {
            return false;
        }
        return size.equals(bucket.size) && origin.equals(bucket.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, origin);
    }

    @Override
    public String toString() {
        return "TimeSeriesBucket[size=" + size + ", origin=" + origin + "]";
    }

    private static BigInteger nanosBetween(Instant start, Instant end) {
        BigInteger seconds = BigInteger.valueOf(end.getEpochSecond())
                .subtract(BigInteger.valueOf(start.getEpochSecond()))
                .multiply(NANOS_PER_SECOND);
        return seconds.add(BigInteger.valueOf((long) end.getNano() - start.getNano()));
    }

    private static BigInteger toNanos(Duration duration) {
        return BigInteger.valueOf(duration.getSeconds())
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(duration.getNano()));
    }
}

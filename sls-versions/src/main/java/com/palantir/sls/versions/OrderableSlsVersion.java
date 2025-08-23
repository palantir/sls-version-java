/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.sls.versions;

import static com.palantir.logsafe.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.palantir.logsafe.UnsafeArg;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;

/**
 * An orderable version string as defined by the [SLS
 * spec](https://github.com/palantir/sls-version-java#sls-product-version-specification).
 */
@Value.Immutable
@ImmutablesStyle
public abstract class OrderableSlsVersion extends SlsVersion implements Comparable<OrderableSlsVersion> {

    @JsonCreator
    public static OrderableSlsVersion valueOf(String value) {
        Optional<OrderableSlsVersion> optional = safeValueOf(value);
        checkArgument(optional.isPresent(), "Not an orderable version: {value}", UnsafeArg.of("value", value));
        return optional.get();
    }

    /** The same as {@link #valueOf(String)}, but will return {@link Optional#empty} if the format is invalid. */
    public static Optional<OrderableSlsVersion> safeValueOf(String value) {
        if (value == null) {
            return Optional.empty();
        }

        MatchResult groups = SlsVersionType.RELEASE.getParser().tryParse(value);
        if (groups != null) {
            return Optional.of(ImmutableOrderableSlsVersion.of(
                    value,
                    groups.groupAsInt(1),
                    groups.groupAsInt(2),
                    groups.groupAsInt(3),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    SlsVersionType.RELEASE));
        }

        groups = SlsVersionType.RELEASE_CANDIDATE.getParser().tryParse(value);
        if (groups != null) {
            OptionalInt rcNumber = OptionalInt.of(groups.groupAsInt(4));
            return Optional.of(ImmutableOrderableSlsVersion.of(
                    value,
                    groups.groupAsInt(1),
                    groups.groupAsInt(2),
                    groups.groupAsInt(3),
                    rcNumber,
                    OptionalInt.empty(),
                    rcNumber,
                    OptionalInt.empty(),
                    SlsVersionType.RELEASE_CANDIDATE));
        }

        groups = SlsVersionType.RELEASE_SNAPSHOT.getParser().tryParse(value);
        if (groups != null) {
            OptionalInt snapshotVersion = OptionalInt.of(groups.groupAsInt(4));
            return Optional.of(ImmutableOrderableSlsVersion.of(
                    value,
                    groups.groupAsInt(1),
                    groups.groupAsInt(2),
                    groups.groupAsInt(3),
                    OptionalInt.empty(),
                    snapshotVersion,
                    snapshotVersion,
                    OptionalInt.empty(),
                    SlsVersionType.RELEASE_SNAPSHOT));
        }

        groups = SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT.getParser().tryParse(value);
        if (groups != null) {
            OptionalInt rcNumber = OptionalInt.of(groups.groupAsInt(4));
            OptionalInt snapshotVersion = OptionalInt.of(groups.groupAsInt(5));
            return Optional.of(ImmutableOrderableSlsVersion.of(
                    value,
                    groups.groupAsInt(1),
                    groups.groupAsInt(2),
                    groups.groupAsInt(3),
                    rcNumber,
                    snapshotVersion,
                    rcNumber,
                    snapshotVersion,
                    SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT));
        }

        return Optional.empty();
    }

    /** Returns true iff the given coordinate has a version which can be parsed into a valid orderable SLS version. */
    public static boolean check(String coordinate) {
        return safeValueOf(coordinate).isPresent();
    }

    @Override
    public final String toString() {
        return getValue();
    }

    @Override
    public final int compareTo(OrderableSlsVersion other) {
        return VersionComparator.INSTANCE.compare(this, other);
    }

    public static class Builder extends ImmutableOrderableSlsVersion.Builder {}
}

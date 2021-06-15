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
import com.fasterxml.jackson.annotation.JsonValue;
import com.palantir.logsafe.UnsafeArg;
import java.util.Optional;
import java.util.regex.Matcher;
import org.immutables.value.Value;

/**
 * An orderable version string as defined by the [SLS
 * spec](https://github.com/palantir/sls-version-java#sls-product-version-specification).
 */
@Value.Immutable
@ImmutablesStyle
public abstract class OrderableSlsVersion extends SlsVersion implements Comparable<OrderableSlsVersion> {

    private static final SlsVersionType[] ORDERED_VERSION_TYPES = {
        SlsVersionType.RELEASE,
        SlsVersionType.RELEASE_CANDIDATE,
        SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT,
        SlsVersionType.RELEASE_SNAPSHOT
    };

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

        SlsVersionType finalType = ORDERED_VERSION_TYPES[0];
        Matcher groups = null;
        for (SlsVersionType type : ORDERED_VERSION_TYPES) {
            Matcher maybeGroups = type.getParser().tryParse(value);
            if (maybeGroups != null) {
                finalType = type;
                groups = maybeGroups;
                break;
            }
        }
        if (groups == null) {
            return Optional.empty();
        }

        OrderableSlsVersion.Builder orderableSlsVersion = new Builder()
                .type(finalType)
                .value(value)
                .majorVersionNumber(Integer.parseInt(groups.group(1)))
                .minorVersionNumber(Integer.parseInt(groups.group(2)))
                .patchVersionNumber(Integer.parseInt(groups.group(3)));

        if (groups.groupCount() >= 4) {
            orderableSlsVersion.firstSequenceVersionNumber(Integer.parseInt(groups.group(4)));
        }
        if (groups.groupCount() >= 5) {
            orderableSlsVersion.secondSequenceVersionNumber(Integer.parseInt(groups.group(5)));
        }

        return Optional.of(orderableSlsVersion.build());
    }

    /** Returns true iff the given coordinate has a version which can be parsed into a valid orderable SLS version. */
    public static boolean check(String coordinate) {
        return safeValueOf(coordinate).isPresent();
    }

    @JsonValue
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

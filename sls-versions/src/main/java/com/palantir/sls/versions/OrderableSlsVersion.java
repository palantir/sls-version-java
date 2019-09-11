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
import static com.palantir.logsafe.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.palantir.logsafe.UnsafeArg;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import org.immutables.value.Value;

/**
 * An orderable version string as defined by the
 * [SLS spec](https://github.com/palantir/sls-version-java/README.md#sls-product-version-specification).
 */
@Value.Immutable
@ImmutablesStyle
public abstract class OrderableSlsVersion extends SlsVersion {

    private static final SlsVersionType[] ORDERED_VERSION_TYPES = {
            SlsVersionType.RELEASE,
            SlsVersionType.RELEASE_CANDIDATE,
            SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT,
            SlsVersionType.RELEASE_SNAPSHOT
    };

    @JsonCreator
    public static OrderableSlsVersion valueOf(String value) {
        Optional<OrderableSlsVersion> optional = safeValueOf(value);
        checkArgument(optional.isPresent(), "Not an orderable version: {value}",
                UnsafeArg.of("value", value));
        return optional.get();
    }

    /**
     * The same as {@link #valueOf(String)}, but will return {@link Optional#empty} if the format is invalid.
     */
    public static Optional<OrderableSlsVersion> safeValueOf(String value) {
        checkNotNull(value, "value cannot be null");
        SlsVersionType type = getTypeOrNull(value);
        if (type == null) {
            return Optional.empty();
        }

        Matcher matcher = type.getPattern().matcher(value);
        matcher.matches();  // without calling matches, the groups are not available.
        OptionalInt firstSequence = matcher.groupCount() > 3
                ? OptionalInt.of(Integer.parseInt(matcher.group(4)))
                : OptionalInt.empty();
        OptionalInt secondSequence = matcher.groupCount() > 4
                ? OptionalInt.of(Integer.parseInt(matcher.group(5)))
                : OptionalInt.empty();

        return Optional.of(new OrderableSlsVersion.Builder()
                .value(value)
                .majorVersionNumber(Integer.parseInt(matcher.group(1)))
                .minorVersionNumber(Integer.parseInt(matcher.group(2)))
                .patchVersionNumber(Integer.parseInt(matcher.group(3)))
                .firstSequenceVersionNumber(firstSequence)
                .secondSequenceVersionNumber(secondSequence)
                .type(type)
                .build());
    }

    // Note: Can avoid duplicate regex evaluate if performance ever becomes a concern.
    private static SlsVersionType getTypeOrNull(String value) {
        for (SlsVersionType type : ORDERED_VERSION_TYPES) {
            if (type.getPattern().matcher(value).matches()) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public final String toString() {
        return getValue();
    }

    public static class Builder extends ImmutableOrderableSlsVersion.Builder {}
}

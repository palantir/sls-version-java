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

@Value.Immutable
@ImmutablesStyle
public abstract class NonOrderableSlsVersion extends SlsVersion {

    @JsonCreator
    public static NonOrderableSlsVersion valueOf(String value) {
        Optional<NonOrderableSlsVersion> optional = safeValueOf(value);
        checkArgument(optional.isPresent(), "Not a non-orderable version: {value}", UnsafeArg.of("value", value));
        return optional.get();
    }

    /** The same as {@link #valueOf(String)}, but will return {@link Optional#empty()} if the format is invalid. */
    public static Optional<NonOrderableSlsVersion> safeValueOf(String value) {
        if (value == null) {
            return Optional.empty();
        }

        Matcher groups = SlsVersionType.NON_ORDERABLE.getParser().tryParse(value);
        if (groups == null) {
            return Optional.empty();
        }

        return Optional.of(new NonOrderableSlsVersion.Builder()
                .value(value)
                .majorVersionNumber(Integer.parseInt(groups.group(1)))
                .minorVersionNumber(Integer.parseInt(groups.group(2)))
                .patchVersionNumber(Integer.parseInt(groups.group(3)))
                .type(SlsVersionType.NON_ORDERABLE)
                .build());
    }

    /**
     * Returns true iff the given coordinate has a version which can be parsed into a valid SLS version, but not an
     * orderable one.
     */
    public static boolean check(String coordinate) {
        return safeValueOf(coordinate).isPresent() && !OrderableSlsVersion.check(coordinate);
    }

    @JsonValue
    @Override
    public final String toString() {
        return getValue();
    }

    public static final class Builder extends ImmutableNonOrderableSlsVersion.Builder {}
}

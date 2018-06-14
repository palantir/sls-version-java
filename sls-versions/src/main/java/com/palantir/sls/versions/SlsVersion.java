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

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;

public abstract class SlsVersion {

    @JsonCreator
    public static SlsVersion valueOf(String value) {
        Optional<OrderableSlsVersion> optionalOrderableVersion = OrderableSlsVersion.safeValueOf(value);
        if (optionalOrderableVersion.isPresent()) {
            return optionalOrderableVersion.get();
        }
        Optional<NonOrderableSlsVersion> optionalNonOrderableVersion = NonOrderableSlsVersion.safeValueOf(value);
        if (optionalNonOrderableVersion.isPresent()) {
            return optionalNonOrderableVersion.get();
        }
        throw new IllegalArgumentException("Value is neither an orderable nor a non-orderable version: " + value);
    }

    /**
     * Returns true iff the given coordinate has a version which can be parsed into a valid SLS string.
     */
    public static boolean check(String coordinate) {
        try {
            valueOf(coordinate);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** The full version string. */
    @Value.Auxiliary
    public abstract String getValue();
    public abstract int getMajorVersionNumber();
    public abstract int getMinorVersionNumber();
    public abstract int getPatchVersionNumber();
    public abstract OptionalInt firstSequenceVersionNumber();
    public abstract OptionalInt secondSequenceVersionNumber();
    public abstract SlsVersionType getType();
}

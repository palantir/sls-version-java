/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

import static com.palantir.logsafe.Preconditions.checkNotNull;

import com.google.common.primitives.Ints;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exists in the test source-set only, because this is a just a reference impl which we use to verify correctness of
 * {@link SlsVersionMatcherParser}.
 */
final class RegexSlsVersionMatcherParser {

    private static final Pattern PATTERN = Pattern.compile("^(([0-9]+|x))\\.(([0-9]+|x))\\.(([0-9]+|x))$");

    private RegexSlsVersionMatcherParser() {}

    public static Optional<SlsVersionMatcher> safeValueOf(String value) {
        checkNotNull(value, "value cannot be null");

        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        } else {
            OptionalInt major = parseInt(matcher.group(1));
            OptionalInt minor = parseInt(matcher.group(3));
            OptionalInt patch = parseInt(matcher.group(5));
            return SlsVersionMatcher.maybeCreate(value, major, minor, patch);
        }
    }

    private static OptionalInt parseInt(String maybeInt) {
        Integer maybeInteger = Ints.tryParse(maybeInt);
        return maybeInteger != null ? OptionalInt.of(maybeInteger) : OptionalInt.empty();
    }
}

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

import java.util.Optional;
import java.util.OptionalInt;

/**
 * A hand-rolled implementation of {@code RegexSlsVersionMatcherParser}.
 */
final class SlsVersionMatcherParser {

    public static Optional<SlsVersionMatcher> safeValueOf(String string) {
        OptionalInt major = OptionalInt.empty();
        OptionalInt minor = OptionalInt.empty();
        OptionalInt patch = OptionalInt.empty();

        // major
        long result = Parsers.numberOrX(string, 0);
        if (Parsers.failed(result)) {
            return Optional.empty(); // reject
        }
        if (Parsers.getResult(result) != Parsers.MAGIC_X_NUMBER) {
            major = OptionalInt.of(Parsers.getResult(result));
        }

        // dot
        result = Parsers.literalDot(string, Parsers.getIndex(result));
        if (Parsers.failed(result)) {
            return Optional.empty();
        }

        // minor
        result = Parsers.numberOrX(string, Parsers.getIndex(result));
        if (Parsers.failed(result)) {
            return Optional.empty(); // reject
        }
        if (Parsers.getResult(result) != Parsers.MAGIC_X_NUMBER) {
            minor = OptionalInt.of(Parsers.getResult(result));
        }

        // dot
        result = Parsers.literalDot(string, Parsers.getIndex(result));
        if (Parsers.failed(result)) {
            return Optional.empty();
        }

        // patch
        result = Parsers.numberOrX(string, Parsers.getIndex(result));
        if (Parsers.failed(result)) {
            return Optional.empty(); // reject
        }
        if (Parsers.getResult(result) != Parsers.MAGIC_X_NUMBER) {
            patch = OptionalInt.of(Parsers.getResult(result));
        }

        if (Parsers.getIndex(result) < string.length()) {
            return Optional.empty(); // reject due to trailing stuff
        }

        return SlsVersionMatcher.maybeCreate(string, major, minor, patch);
    }

    private SlsVersionMatcherParser() {}
}

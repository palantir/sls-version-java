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
 * A hand-rolled implementation of {@link RegexSlsVersionMatcherParser}.
 *
 * Uses two-element int[] arrays as the 'result' type to represent a successful/failed parse, where result[0] always
 * contains the index into the string we're looking at, and result[1] contains the value returned by the parser
 * combinator.
 */
final class SlsVersionMatcherParser {

    private static final int MAGIC_X_NUMBER = -1;

    public static Optional<SlsVersionMatcher> safeValueOf(String string) {
        OptionalInt major = OptionalInt.empty();
        OptionalInt minor = OptionalInt.empty();
        OptionalInt patch = OptionalInt.empty();

        int[] result = new int[] {0};

        // major
        result = numberOrX(string, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != MAGIC_X_NUMBER) {
            major = OptionalInt.of(result[1]);
        }

        // dot
        result = literalDot(string, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty();
        }

        // minor
        result = numberOrX(string, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != MAGIC_X_NUMBER) {
            minor = OptionalInt.of(result[1]);
        }

        // dot
        result = literalDot(string, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty();
        }

        // patch
        result = numberOrX(string, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != MAGIC_X_NUMBER) {
            patch = OptionalInt.of(result[1]);
        }

        if (result[0] < string.length()) {
            return Optional.empty(); // reject due to trailing stuff
        }

        return SlsVersionMatcher.maybeCreate(string, major, minor, patch);
    }

    // "x" is signified by the magic negative number -1, which is distinct from Integer.MIN_VALUE which is a failure
    private static int[] numberOrX(String string, int startIndex) {
        int[] result = literalX(string, startIndex);
        if (result[1] != Integer.MIN_VALUE) {
            result[1] = MAGIC_X_NUMBER;
            return result;
        }

        result = number(string, startIndex);
        if (result[1] != Integer.MIN_VALUE) {
            return result;
        }

        return new int[] {startIndex, Integer.MIN_VALUE};
    }

    private static int[] number(String string, int startIndex) {
        int next = startIndex;
        int len = string.length();
        while (next < len) {
            int codepoint = string.codePointAt(next);
            if (Character.isDigit(codepoint)) {
                next += 1;
            } else {
                break;
            }
        }
        if (next == startIndex) {
            return new int[] {startIndex, Integer.MIN_VALUE};
        } else if (next == startIndex + 1) {
            return new int[] {next, Character.digit(string.codePointAt(startIndex), 10)};
        } else {
            try {
                return new int[] {next, Integer.parseUnsignedInt(string.substring(startIndex, next))};
            } catch (NumberFormatException e) {
                if (e.getMessage().endsWith("exceeds range of unsigned int.")) {
                    return new int[] {startIndex, Integer.MIN_VALUE};
                } else {
                    throw e;
                }
            }
        }
    }

    // 0 signifies success
    private static int[] literalX(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == 'x') {
            return new int[] {startIndex + 1, 0};
        } else {
            return new int[] {startIndex, Integer.MIN_VALUE};
        }
    }

    private static int[] literalDot(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == '.') {
            return new int[] {startIndex + 1, 0};
        } else {
            return new int[] {startIndex, Integer.MIN_VALUE};
        }
    }

    private SlsVersionMatcherParser() {}
}

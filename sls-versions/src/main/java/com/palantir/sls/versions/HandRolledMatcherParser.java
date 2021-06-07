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
 * A hand-rolled implementation of the regex ^([0-9]+|x)\.([0-9]+|x)\.([0-9]+|x)$.
 *
 * Uses two-element int[] arrays as the 'result' type to represent a successful/failed parse, where result[0] always
 * contains the index into the string we're looking at, and result[1] contains the value returned by the parser
 * combinator.
 */
final class HandRolledMatcherParser {

    public static Optional<SlsVersionMatcher> safeValueOf(String s) {
        OptionalInt major = OptionalInt.empty();
        OptionalInt minor = OptionalInt.empty();
        OptionalInt patch = OptionalInt.empty();

        int[] result = new int[] {0, Integer.MIN_VALUE};

        // major
        result = numberOrX(s, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != -1) {
            major = OptionalInt.of(result[1]);
        }

        // dot
        result = literalDot(s, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty();
        }

        // minor
        result = numberOrX(s, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != -1) {
            minor = OptionalInt.of(result[1]);
        }

        // dot
        result = literalDot(s, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty();
        }

        // patch
        result = numberOrX(s, result[0]);
        if (result[1] == Integer.MIN_VALUE) {
            return Optional.empty(); // reject
        }
        if (result[1] != -1) {
            patch = OptionalInt.of(result[1]);
        }

        if (result[0] < s.length()) {
            return Optional.empty(); // reject due to trailing stuff
        }

        return SlsVersionMatcher.maybeCreate(s, major, minor, patch);
    }

    //  "x" is signified by the magic negative number -1, which is distinct from Integer.MIN_VALUE which is a failure
    public static int[] numberOrX(String s, int i) {
        int[] result = literalX(s, i);
        if (result[1] != Integer.MIN_VALUE) {
            result[1] = -1;
            return result;
        }

        result = number(s, i);
        if (result[1] != Integer.MIN_VALUE) {
            return result;
        }

        return new int[] {i, Integer.MIN_VALUE};
    }

    private static int[] number(String s, int startIndex) {
        int next = startIndex;
        int len = s.length();
        while (next < len) {
            int codepoint = s.codePointAt(next);
            if (Character.isDigit(codepoint)) {
                next += 1;
            } else {
                break;
            }
        }
        if (next == startIndex) {
            return new int[] {startIndex, Integer.MIN_VALUE};
        } else if (next == startIndex + 1) {
            return new int[] {next, Character.getNumericValue(s.codePointAt(startIndex))};
        } else {
            return new int[] {next, Integer.parseInt(s.substring(startIndex, next))};
        }
    }

    // 0 signifies success
    private static int[] literalX(String s, int startIndex) {
        if (s.codePointAt(startIndex) == 'x') {
            return new int[] {startIndex + 1, 0};
        } else {
            return new int[] {startIndex, Integer.MIN_VALUE};
        }
    }

    private static int[] literalDot(String s, int startIndex) {
        if (s.codePointAt(startIndex) == '.') {
            return new int[] {startIndex + 1, 0};
        } else {
            return new int[] {startIndex, Integer.MIN_VALUE};
        }
    }

    private HandRolledMatcherParser() {}
}

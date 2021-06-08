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
 *
 * We're using parser combinator-style ideas here, where each parser function {@link #numberOrX}, {@link #number},
 * {@link #literalX} accepts an index into our source string and returns a two values:
 *
 * - an updated index into the string representing how many characters were parsed
 * - a value that was actually parsed out of the string (if the parser was able to parse the string), otherwise a
 * clear signal that the parser failed (we use {@link Integer#MIN_VALUE} for this.
 *
 * We bit-pack these two integer values into a single long using {@link #ok} and {@link #fail} functions because
 * primitive longs live on the stack and don't impact GC.
 */
final class SlsVersionMatcherParser {

    private static final int MAGIC_X_NUMBER = -1;

    public static Optional<SlsVersionMatcher> safeValueOf(String string) {
        OptionalInt major = OptionalInt.empty();
        OptionalInt minor = OptionalInt.empty();
        OptionalInt patch = OptionalInt.empty();

        // major
        long result = numberOrX(string, 0);
        if (failed(result)) {
            return Optional.empty(); // reject
        }
        if (getResult(result) != MAGIC_X_NUMBER) {
            major = OptionalInt.of(getResult(result));
        }

        // dot
        result = literalDot(string, getIndex(result));
        if (failed(result)) {
            return Optional.empty();
        }

        // minor
        result = numberOrX(string, getIndex(result));
        if (failed(result)) {
            return Optional.empty(); // reject
        }
        if (getResult(result) != MAGIC_X_NUMBER) {
            minor = OptionalInt.of(getResult(result));
        }

        // dot
        result = literalDot(string, getIndex(result));
        if (failed(result)) {
            return Optional.empty();
        }

        // patch
        result = numberOrX(string, getIndex(result));
        if (failed(result)) {
            return Optional.empty(); // reject
        }
        if (getResult(result) != MAGIC_X_NUMBER) {
            patch = OptionalInt.of(getResult(result));
        }

        if (getIndex(result) < string.length()) {
            return Optional.empty(); // reject due to trailing stuff
        }

        return SlsVersionMatcher.maybeCreate(string, major, minor, patch);
    }

    // "x" is signified by the magic negative number -1, which is distinct from Integer.MIN_VALUE which is a failure
    private static long numberOrX(String string, int startIndex) {
        long xResult = literalX(string, startIndex);
        if (isOk(xResult)) {
            return ok(getIndex(xResult), MAGIC_X_NUMBER);
        }

        long numberResult = number(string, startIndex);
        if (isOk(numberResult)) {
            return numberResult;
        }

        return fail(startIndex);
    }

    private static long number(String string, int startIndex) {
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
            return fail(startIndex);
        } else if (next == startIndex + 1) {
            return ok(next, Character.digit(string.codePointAt(startIndex), 10));
        } else {
            try {
                return ok(next, Integer.parseUnsignedInt(string.substring(startIndex, next)));
            } catch (NumberFormatException e) {
                if (e.getMessage().endsWith("exceeds range of unsigned int.")) {
                    return fail(startIndex);
                } else {
                    throw e;
                }
            }
        }
    }

    // 0 signifies success
    private static long literalX(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == 'x') {
            return ok(startIndex + 1, 0);
        } else {
            return fail(startIndex);
        }
    }

    private static long literalDot(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == '.') {
            return ok(startIndex + 1, 0);
        } else {
            return fail(startIndex);
        }
    }

    private static final long INT_MASK = (1L << 32) - 1;

    /**
     * We are bit-packing two integers into a single long.  The 'index' occupies half of the bits and the 'result'
     * occupies the other half.
     */
    static long ok(int index, int result) {
        return ((long) index) << 32 | (result & INT_MASK);
    }

    static long fail(int index) {
        return ((long) index) << 32 | (Integer.MIN_VALUE & INT_MASK);
    }

    static boolean isOk(long state) {
        return getResult(state) != Integer.MIN_VALUE;
    }

    static boolean failed(long state) {
        return !isOk(state);
    }

    static int getResult(long state) {
        return (int) (state & INT_MASK);
    }

    static int getIndex(long state) {
        return (int) (state >>> 32);
    }

    private SlsVersionMatcherParser() {}
}

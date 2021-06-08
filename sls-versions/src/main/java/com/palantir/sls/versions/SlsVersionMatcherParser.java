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

    private static final int MAGIC_X_NUMBER = -1;

    public static Optional<SlsVersionMatcher> safeValueOf(String string) {
        OptionalInt major = OptionalInt.empty();
        OptionalInt minor = OptionalInt.empty();
        OptionalInt patch = OptionalInt.empty();

        // major
        ParseResult result = numberOrX(string, 0);
        if (result.failed()) {
            return Optional.empty(); // reject
        }
        if (result.result != MAGIC_X_NUMBER) {
            major = OptionalInt.of(result.result);
        }

        // dot
        result = literalDot(string, result.index);
        if (result.failed()) {
            return Optional.empty();
        }

        // minor
        result = numberOrX(string, result.index);
        if (result.failed()) {
            return Optional.empty(); // reject
        }
        if (result.result != MAGIC_X_NUMBER) {
            minor = OptionalInt.of(result.result);
        }

        // dot
        result = literalDot(string, result.index);
        if (result.failed()) {
            return Optional.empty();
        }

        // patch
        result = numberOrX(string, result.index);
        if (result.failed()) {
            return Optional.empty(); // reject
        }
        if (result.result != MAGIC_X_NUMBER) {
            patch = OptionalInt.of(result.result);
        }

        if (result.index < string.length()) {
            return Optional.empty(); // reject due to trailing stuff
        }

        return SlsVersionMatcher.maybeCreate(string, major, minor, patch);
    }

    // "x" is signified by the magic negative number -1, which is distinct from Integer.MIN_VALUE which is a failure
    private static ParseResult numberOrX(String string, int startIndex) {
        ParseResult xResult = literalX(string, startIndex);
        if (xResult.isOk()) {
            return ParseResult.ok(xResult.index, MAGIC_X_NUMBER);
        }

        ParseResult numberResult = number(string, startIndex);
        if (numberResult.isOk()) {
            return numberResult;
        }

        return ParseResult.fail(startIndex);
    }

    private static ParseResult number(String string, int startIndex) {
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
            return ParseResult.fail(startIndex);
        } else if (next == startIndex + 1) {
            return ParseResult.ok(next, Character.digit(string.codePointAt(startIndex), 10));
        } else {
            try {
                return ParseResult.ok(next, Integer.parseUnsignedInt(string.substring(startIndex, next)));
            } catch (NumberFormatException e) {
                if (e.getMessage().endsWith("exceeds range of unsigned int.")) {
                    return ParseResult.fail(startIndex);
                } else {
                    throw e;
                }
            }
        }
    }

    // 0 signifies success
    private static ParseResult literalX(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == 'x') {
            return ParseResult.ok(startIndex + 1, 0);
        } else {
            return ParseResult.fail(startIndex);
        }
    }

    private static ParseResult literalDot(String string, int startIndex) {
        if (startIndex < string.length() && string.codePointAt(startIndex) == '.') {
            return ParseResult.ok(startIndex + 1, 0);
        } else {
            return ParseResult.fail(startIndex);
        }
    }

    static final class ParseResult {
        private final int index;
        private final int result;

        private ParseResult(int index, int result) {
            this.index = index;
            this.result = result;
        }

        static ParseResult ok(int index, int result) {
            return new ParseResult(index, result);
        }

        static ParseResult fail(int index) {
            return new ParseResult(index, Integer.MIN_VALUE);
        }

        boolean isOk() {
            return result != Integer.MIN_VALUE;
        }

        boolean failed() {
            return result == Integer.MIN_VALUE;
        }
    }

    private SlsVersionMatcherParser() {}
}

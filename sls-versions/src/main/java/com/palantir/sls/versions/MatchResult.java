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

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.util.regex.Matcher;

interface MatchResult {

    /** 1-indexed, to match java regexes. */
    int groupAsInt(int group);

    int groupCount();

    final class RegexMatchResult implements MatchResult {
        private static final int RADIX = 10;
        private final String string;
        private final Matcher matcher;

        RegexMatchResult(String string, Matcher matcher) {
            this.string = string;
            this.matcher = matcher;
        }

        @Override
        public int groupAsInt(int group) {
            int groupStart = matcher.start(group);
            int groupEnd = matcher.end(group);
            if (groupStart == -1) {
                throw new NumberFormatException();
            }

            int integer = Integer.parseUnsignedInt(string, groupStart, groupEnd, RADIX);
            if (integer < 0) {
                // Extracted to a function to help inlining, as this is probably very uncommon.
                throw safeIllegalStateException(groupStart, groupEnd);
            }
            return integer;
        }

        @Override
        public int groupCount() {
            return matcher.groupCount();
        }

        private SafeIllegalStateException safeIllegalStateException(int groupStart, int groupEnd) {
            return new SafeIllegalStateException(
                    "Can't parse segment as integer as it overflowed",
                    SafeArg.of("string", string),
                    SafeArg.of("segment", string.substring(groupStart, groupEnd)));
        }
    }

    final class Int3MatchResult implements MatchResult {
        private final int first;
        private final int second;
        private final int third;

        Int3MatchResult(int first, int second, int third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public int groupAsInt(int group) {
            switch (group) {
                case 1:
                    return first;
                case 2:
                    return second;
                case 3:
                    return third;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int groupCount() {
            return 3;
        }
    }
}

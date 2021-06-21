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

            if (groupEnd - groupStart == 1) {
                return Character.digit(string.codePointAt(groupStart), RADIX);
            }

            return Integer.parseUnsignedInt(string, groupStart, groupEnd, RADIX);
        }

        @Override
        public int groupCount() {
            return matcher.groupCount();
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

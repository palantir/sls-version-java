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

import com.google.errorprone.annotations.Immutable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * This class encourages callers to call the parsing logic once, rather than fall into the trap of accidentally calling
 * {@link Matcher#matches} multiple times.
 */
@Immutable
final class RegexParser implements Parser {
    private final Pattern pattern;

    RegexParser(Pattern pattern) {
        this.pattern = pattern;
    }

    static RegexParser of(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new RegexParser(pattern);
    }

    /** Returns a {@link MatchResult} if the provided string matches the pattern, or null otherwise. */
    @Override
    @Nullable
    public MatchResult tryParse(String string) {
        Matcher matcher = pattern.matcher(string);
        return matcher.matches() ? new MatchResult.RegexMatchResult(string, matcher) : null;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }
}

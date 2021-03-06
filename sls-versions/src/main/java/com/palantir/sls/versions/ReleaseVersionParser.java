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
import com.palantir.sls.versions.MatchResult.Int3MatchResult;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@Immutable
enum ReleaseVersionParser implements Parser {
    INSTANCE;

    @Nullable
    @Override
    public MatchResult tryParse(String string) {
        long state = Parsers.number(string, 0);
        if (Parsers.failed(state)) {
            return null;
        }
        int first = Parsers.getResult(state);

        state = Parsers.literalDot(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            return null;
        }

        state = Parsers.number(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            return null;
        }
        int second = Parsers.getResult(state);

        state = Parsers.literalDot(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            return null;
        }

        state = Parsers.number(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            return null;
        }
        int third = Parsers.getResult(state);

        if (Parsers.getIndex(state) < string.length()) {
            return null; // reject due to trailing stuff
        }

        return new Int3MatchResult(first, second, third);
    }

    @Override
    public Pattern getPattern() {
        return Pattern.compile("^([0-9]+)\\.([0-9]+)\\.([0-9]+)$");
    }
}

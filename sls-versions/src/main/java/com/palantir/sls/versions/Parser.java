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
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@Immutable
interface Parser {

    /** Returns a {@link MatchResult} if the provided string matches the pattern, or null otherwise. */
    @Nullable
    MatchResult tryParse(String string);

    /** An equivalent java regex. */
    Pattern getPattern();
}

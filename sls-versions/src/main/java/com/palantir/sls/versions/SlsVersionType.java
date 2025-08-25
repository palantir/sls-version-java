/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

import java.util.regex.Pattern;

/**
 * Defines the available types of SLS versions together with regular expressions for parsing strings into corresponding
 * {@link SlsVersion} objects.
 */
public enum SlsVersionType {
    RELEASE_SNAPSHOT("^([0-9]+)\\.([0-9]+)\\.([0-9]+)-([0-9]+)-g[a-f0-9]+$", 4),
    RELEASE("^([0-9]+)\\.([0-9]+)\\.([0-9]+)$", 3),
    RELEASE_CANDIDATE_SNAPSHOT("^([0-9]+)\\.([0-9]+)\\.([0-9]+)-rc([0-9]+)-([0-9]+)-g[a-f0-9]+$", 2),
    RELEASE_CANDIDATE("^([0-9]+)\\.([0-9]+)\\.([0-9]+)-rc([0-9]+)$", 1),
    NON_ORDERABLE("^([0-9]+)\\.([0-9]+)\\.([0-9]+)(-[a-z0-9-]+)?(\\.dirty)?$", 0);

    private final Pattern pattern;
    private final int priority;

    SlsVersionType(String regex, int priority) {
        this.pattern = Pattern.compile(regex);
        this.priority = priority;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * The priority was used to have a predetermined order of version types, to ease comparison,
     *   but suffered problems due to RELEASE_CANDIDATE_SNAPSHOT and RELEASE_CANDIDATE versions not being systematically
     *   ordered (e.g. 1.2.3-rc1 < 1.2.3-rc1-1-gabc < 1.2.3-rc2 < 1.2.3-rc2-1-gdef).
     * @deprecated This field is deprecated and will be removed in a future release.
     */
    @Deprecated(forRemoval = true)
    public int getPriority() {
        return this.priority;
    }

    public boolean isSnapshot() {
        return this == RELEASE_SNAPSHOT || this == RELEASE_CANDIDATE_SNAPSHOT;
    }

    public boolean isReleaseCandidate() {
        return this == RELEASE_CANDIDATE || this == RELEASE_CANDIDATE_SNAPSHOT;
    }
}

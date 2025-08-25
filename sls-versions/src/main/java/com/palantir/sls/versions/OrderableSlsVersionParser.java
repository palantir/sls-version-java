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

import com.palantir.logsafe.Preconditions;
import javax.annotation.Nullable;

enum OrderableSlsVersionParser {
    INSTANCE;

    @Nullable
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public OrderableSlsVersion tryParse(String string) {
        long state = Parsers.number(string, 0);
        if (Parsers.failed(state)) {
            // Doesn't start with a number
            return null;
        }
        int major = Parsers.getResult(state);

        state = Parsers.literalDot(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a dot after the major version number
            return null;
        }

        state = Parsers.number(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a minor version number
            return null;
        }
        int minor = Parsers.getResult(state);

        state = Parsers.literalDot(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a dot after the minor version number
            return null;
        }

        state = Parsers.number(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a patch version number
            return null;
        }
        int patch = Parsers.getResult(state);

        if (Parsers.getIndex(state) == string.length()) {
            // Successfully reached the end of the string after parsing major.minor.patch
            return new OrderableSlsVersion.Builder()
                    .value(string)
                    .type(SlsVersionType.RELEASE)
                    .majorVersionNumber(major)
                    .minorVersionNumber(minor)
                    .patchVersionNumber(patch)
                    .build();
        }

        int rc = -1;
        boolean isRc = false;
        // Store the state in a different variable, so the index doesn't get moved if -rc is not present
        long rcState = Parsers.literalDashRc(string, Parsers.getIndex(state));
        if (Parsers.isOk(rcState)) {
            // Found -rc, so this is either a release candidate or release candidate snapshot
            isRc = true;
            rcState = Parsers.number(string, Parsers.getIndex(rcState));
            if (Parsers.failed(rcState)) {
                // Doesn't have a number after -rc
                return null;
            }
            rc = Parsers.getResult(rcState);
            // Move index past the rc part, to parse snapshot if present
            state = rcState;
        }

        if (Parsers.getIndex(state) == string.length()) {
            // Successfully reached the end of the string after parsing major.minor.patch-rcX
            // If isRc is false, this means we didn't have -rc after the patch, and state is the same as before
            //   which we already checked isn't with an index at string.length(), therefor this shouldn't happen
            Preconditions.checkState(isRc, "Unexpected error - rc should be present");
            return new OrderableSlsVersion.Builder()
                    .value(string)
                    .type(SlsVersionType.RELEASE_CANDIDATE)
                    .majorVersionNumber(major)
                    .minorVersionNumber(minor)
                    .patchVersionNumber(patch)
                    .rcNumber(rc)
                    .build();
        }

        state = Parsers.literalDash(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a dash after either the patch version number or the rc number
            return null;
        }

        state = Parsers.number(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a snapshot number after the dash
            return null;
        }
        int snapshot = Parsers.getResult(state);

        state = Parsers.literalDashG(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have -g after the snapshot number
            return null;
        }

        state = Parsers.hexString(string, Parsers.getIndex(state));
        if (Parsers.failed(state)) {
            // Doesn't have a hex string after -g
            return null;
        }

        if (Parsers.getIndex(state) < string.length()) {
            // There are still some trailing characters after parsing the whole version
            return null;
        }

        if (isRc) {
            return new OrderableSlsVersion.Builder()
                    .value(string)
                    .type(SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT)
                    .majorVersionNumber(major)
                    .minorVersionNumber(minor)
                    .patchVersionNumber(patch)
                    .rcNumber(rc)
                    .snapshotNumber(snapshot)
                    .build();
        } else {
            return new OrderableSlsVersion.Builder()
                    .value(string)
                    .type(SlsVersionType.RELEASE_SNAPSHOT)
                    .majorVersionNumber(major)
                    .minorVersionNumber(minor)
                    .patchVersionNumber(patch)
                    .snapshotNumber(snapshot)
                    .build();
        }
    }
}

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

import java.util.Comparator;

/** Compares {@link OrderableSlsVersion}s by "newness", i.e., "1.4.0" is greater/newer/later than "1.2.1", etc.. */
public enum VersionComparator implements Comparator<OrderableSlsVersion> {
    INSTANCE;

    @Override
    @SuppressWarnings("ImmutablesReferenceEquality")
    public int compare(OrderableSlsVersion version1, OrderableSlsVersion version2) {
        if (version1 == version2) {
            return 0;
        }

        int result = Integer.compare(version1.getMajorVersionNumber(), version2.getMajorVersionNumber());
        if (result != 0) {
            return result;
        }

        result = Integer.compare(version1.getMinorVersionNumber(), version2.getMinorVersionNumber());
        if (result != 0) {
            return result;
        }

        result = Integer.compare(version1.getPatchVersionNumber(), version2.getPatchVersionNumber());
        if (result != 0) {
            return result;
        }

        result = Integer.compare(
                version1.getType().getPriority(), version2.getType().getPriority());
        if (result != 0) {
            return result;
        }

        Integer maybeFirstSequence1 = version1.getFirstSequenceVersionNumber();
        Integer maybeFirstSequence2 = version2.getFirstSequenceVersionNumber();
        result = Integer.compare(
                maybeFirstSequence1 == null ? 0 : maybeFirstSequence1,
                maybeFirstSequence2 == null ? 0 : maybeFirstSequence2);
        if (result != 0) {
            return result;
        }

        Integer maybeSecondSequence1 = version1.getSecondSequenceVersionNumber();
        Integer maybeSecondSequence2 = version2.getSecondSequenceVersionNumber();
        result = Integer.compare(
                maybeSecondSequence1 == null ? 0 : maybeSecondSequence1,
                maybeSecondSequence2 == null ? 0 : maybeSecondSequence2);
        if (result != 0) {
            return result;
        }

        return 0;
    }
}

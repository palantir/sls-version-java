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

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.util.Comparator;

/** Compares {@link OrderableSlsVersion}s by "newness", i.e., "1.4.0" is greater/newer/later than "1.2.1", etc.. */
public enum VersionComparator implements Comparator<OrderableSlsVersion> {
    INSTANCE;

    private static final Comparator<OrderableSlsVersion> majorMinorPatchComparator = Comparator.comparing(
                    OrderableSlsVersion::getMajorVersionNumber)
            .thenComparing(OrderableSlsVersion::getMinorVersionNumber)
            .thenComparing(OrderableSlsVersion::getPatchVersionNumber);

    private static final Comparator<OrderableSlsVersion> typePriority =
            Comparator.comparingInt(v -> v.getType().getPriority());
    private static final Comparator<OrderableSlsVersion> firstSequence =
            Comparator.comparingInt(v -> v.firstSequenceVersionNumber()
                    .orElseThrow(() -> new SafeIllegalArgumentException(
                            "Expected to find a first sequence number for version",
                            SafeArg.of("version", v.getValue()))));

    private static final Comparator<OrderableSlsVersion> secondSequence =
            // Substitute -1 if not present because snapshots are greater than non-snapshots.
            Comparator.comparingInt(v -> v.secondSequenceVersionNumber().orElse(-1));

    public static Comparator<OrderableSlsVersion> majorMinorPatch() {
        return majorMinorPatchComparator;
    }

    @Override
    public int compare(OrderableSlsVersion left, OrderableSlsVersion right) {
        if (left.getValue().equals(right.getValue())) {
            return 0;
        }

        int mainVersionComparison = majorMinorPatch().compare(left, right);
        if (mainVersionComparison != 0) {
            return mainVersionComparison;
        }

        if ((left.getType() == SlsVersionType.RELEASE) || (right.getType() == SlsVersionType.RELEASE)) {
            // Releases always compare correctly just by type now we know base version matches.
            return typePriority.compare(left, right);
        }

        if ((left.getType() == SlsVersionType.RELEASE_SNAPSHOT)
                && (right.getType() == SlsVersionType.RELEASE_SNAPSHOT)) {
            // If both are snapshots, compare snapshot number.

            return firstSequence.compare(left, right);
        }

        if (left.getType() == SlsVersionType.RELEASE_SNAPSHOT) {
            // Snapshot is larger.
            return 1;
        }

        if (right.getType() == SlsVersionType.RELEASE_SNAPSHOT) {
            // Snapshot is larger.
            return -1;
        }

        return compareSuffix(left, right);
    }

    private int compareSuffix(OrderableSlsVersion left, OrderableSlsVersion right) {
        // We know by this point that both are RCs or RC-snapshots.
        // Compare RC number first.

        int rcCompare = firstSequence.compare(left, right);
        if (rcCompare != 0) {
            return rcCompare;
        }

        // RC number is the same, compare snapshot versions.
        // Substitute -1 if not present because snapshots are greater than non-snapshots.
        return secondSequence.compare(left, right);
    }
}

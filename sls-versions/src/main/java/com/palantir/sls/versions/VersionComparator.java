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

import static com.palantir.logsafe.Preconditions.checkArgument;

import com.palantir.logsafe.SafeArg;
import java.util.Comparator;
import java.util.OptionalInt;

/** Compares {@link OrderableSlsVersion}s by "newness", i.e., "1.4.0" is greater/newer/later than "1.2.1", etc.. */
public enum VersionComparator implements Comparator<OrderableSlsVersion> {
    INSTANCE;

    @Override
    public int compare(OrderableSlsVersion left, OrderableSlsVersion right) {
        if (left.getValue().equals(right.getValue())) {
            return 0;
        }

        int mainVersionComparison = compareMainVersion(left, right);
        if (mainVersionComparison != 0) {
            return mainVersionComparison;
        }

        if ((left.getType() == SlsVersionType.RELEASE) || (right.getType() == SlsVersionType.RELEASE)) {
            // Releases always compare correctly just by type now we know base version matches.
            return Integer.compare(left.getType().getPriority(), right.getType().getPriority());
        }

        if ((left.getType() == SlsVersionType.RELEASE_SNAPSHOT)
                && (right.getType() == SlsVersionType.RELEASE_SNAPSHOT)) {
            // If both are snapshots, compare snapshot number.
            return compareFirstSequenceVersions(left, right);
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

    private int compareMainVersion(OrderableSlsVersion left, OrderableSlsVersion right) {
        if (left.getMajorVersionNumber() != right.getMajorVersionNumber()) {
            return left.getMajorVersionNumber() > right.getMajorVersionNumber() ? 1 : -1;
        }

        if (left.getMinorVersionNumber() != right.getMinorVersionNumber()) {
            return left.getMinorVersionNumber() > right.getMinorVersionNumber() ? 1 : -1;
        }

        if (left.getPatchVersionNumber() != right.getPatchVersionNumber()) {
            return left.getPatchVersionNumber() > right.getPatchVersionNumber() ? 1 : -1;
        }

        return 0;
    }

    private int compareSuffix(OrderableSlsVersion left, OrderableSlsVersion right) {
        // We know by this point that both are RCs or RC-snapshots.
        // Compare RC number first.
        int rcCompare = compareFirstSequenceVersions(left, right);
        if (rcCompare != 0) {
            return rcCompare;
        }

        // RC number is the same, compare snapshot versions.
        // Substitute -1 if not present because snapshots are greater than non-snapshots.
        OptionalInt leftInt = left.secondSequenceVersionNumber();
        OptionalInt rightInt = right.secondSequenceVersionNumber();
        int secondSequenceCompare = Integer.compare(leftInt.orElse(-1), rightInt.orElse(-1));
        if (secondSequenceCompare != 0) {
            return secondSequenceCompare;
        }

        return stringCompare(left, right);
    }

    private int compareFirstSequenceVersions(OrderableSlsVersion left, OrderableSlsVersion right) {
        OptionalInt leftInt = left.firstSequenceVersionNumber();
        OptionalInt rightInt = right.firstSequenceVersionNumber();

        checkArgument(
                leftInt.isPresent(),
                "Expected to find a first sequence number for version",
                SafeArg.of("version", left.getValue()));
        checkArgument(
                rightInt.isPresent(),
                "Expected to find a first sequence number for version",
                SafeArg.of("version", right.getValue()));

        int firstSequenceCompare = Integer.compare(leftInt.getAsInt(), rightInt.getAsInt());
        if (firstSequenceCompare != 0) {
            return firstSequenceCompare;
        }

        return stringCompare(left, right);
    }

    private int stringCompare(OrderableSlsVersion left, OrderableSlsVersion right) {
        // We know everything but the hash is equal between the two versions.
        // We fall back to string based comparison to settle potential differences between git hashes.
        return left.toString().compareTo(right.toString());
    }
}

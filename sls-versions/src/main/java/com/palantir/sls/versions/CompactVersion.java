/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.google.errorprone.annotations.CompileTimeConstant;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.util.OptionalInt;

/**
 * Stores a compact representation of {@link OrderableSlsVersion} that is lexicographically ordered when rendered as
 * a sequence of bytes. CompactVersion allocates 20 bits (for a maximum value of 1,048,575) to store each of the five
 * numeric components of an {@link OrderableSlsVersion}. To enable representing bytes as two SafeLong values, this
 * implementation packs at most 53 bits into a single {@code long}.
 *
 * <p>Bits are allocated as follows, from lowest bits to highest:
 * <code>
 * LSB SafeLong:
 * 20 bits: distance from release
 *  2 bits: priority1 (1 = RELEASE_CANDIDATE_SNAPSHOT, 0 = else; 2 and 3 are unused values)
 * 20 bits: RC number
 *  2 bits: priority2 (2 = RELEASE_SNAPSHOT, 1 = RELEASE, 0 = RELEASE_CANDIDATE; 3 is an unused value)
 *  8 bits: lowest 8 bits of patch
 *
 * MSB SafeLong:
 * 12 bits: highest 12 bits of patch
 * 20 bits: minor
 * 20 bits: major
 * </code>
 *
 * <p><b>Note</b>: the correctness of the implementation of {@link #compareTo(CompactVersion)} depends on the
 * constituent longs holding only positive values. This is partially accomplished by using less than the full number of
 * bits available, and thus by avoiding twos-complement representation issues when the highest bit in the long is set.
 */
public final class CompactVersion implements Comparable<CompactVersion> {
    private static final int MASK_20_BITS = 0xFFFFF;
    private static final int MASK_12_BITS = 0xFFF;
    private static final int MASK_8_BITS = 0xFF;
    private static final int MASK_2_BITS = 0x3;

    private final long msb;
    private final long lsb;

    private CompactVersion(long msb, long lsb) {
        this.msb = msb;
        this.lsb = lsb;
    }

    public long getMsb() {
        return msb;
    }

    public long getLsb() {
        return lsb;
    }

    public static CompactVersion from(OrderableSlsVersion version) {
        long patch = encode20b(version.getPatchVersionNumber(), "patch");

        int rcNumber = version.firstSequenceVersionNumber().orElse(0);
        int distanceFromVersion = version.secondSequenceVersionNumber().orElse(0);
        if (version.getType().equals(SlsVersionType.RELEASE_SNAPSHOT)) {
            // in this version format (1.0.0-10-gaaaaaa), the first sequence number represents the distance
            // from the version rather than the implicit RC number
            rcNumber = 0;
            distanceFromVersion = version.firstSequenceVersionNumber().orElse(0);
        }

        long lsb = encode20b(distanceFromVersion, "distanceFromVersion")
                + (encodePriority1(version.getType()) << 20)
                + (encode20b(rcNumber, "rcNumber") << 22)
                + (encodePriority2(version.getType()) << 42)
                + ((patch & 0xFF) << 44);
        long msb = ((patch & 0xFFF00) >> 8)
                + (encode20b(version.getMinorVersionNumber(), "minor") << 12)
                + (encode20b(version.getMajorVersionNumber(), "major") << 32);

        return new CompactVersion(msb, lsb);
    }

    private static long encodePriority1(SlsVersionType type) {
        return type.equals(SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT) ? 1 : 0;
    }

    private static long encodePriority2(SlsVersionType type) {
        switch (type) {
            case RELEASE_SNAPSHOT:
                return 2;
            case RELEASE:
                return 1;
            case RELEASE_CANDIDATE:
            case RELEASE_CANDIDATE_SNAPSHOT:
                return 0;
            case NON_ORDERABLE:
                throw new SafeIllegalArgumentException("Unable to store NON_ORDERABLE types in CompactVersion");
        }
        throw new SafeIllegalArgumentException("Unknown SlsVersionType", SafeArg.of("slsVersionType", type));
    }

    private static long encode20b(int value, @CompileTimeConstant final String component) {
        Preconditions.checkArgument(value >= 0 && value < 1_048_576,
                "version component must be positive and not exceed 20 bits of value",
                SafeArg.of(component, value));
        return value & MASK_20_BITS;
    }

    /**
     * Returns an {@link OrderableSlsVersion} equivalent to this object.
     *
     * <p><b>Note</b>: Snapshot versions <i>must</i> include a git hash in the string representation, but because
     * {@link OrderableSlsVersion} does not require equality and because this class' compact representation does not
     * store the string, the git hash will always be set to {@code gaaaaaa}.
     */
    public OrderableSlsVersion toSlsVersion() {
        int majorVersionNumber = (int) (msb >> 32) & MASK_20_BITS;
        int minorVersionNumber = (int) (msb >> 12) & MASK_20_BITS;
        int patchVersionNumber = (int) ((msb & MASK_12_BITS) << 8) + (int) ((lsb >> 44) & MASK_8_BITS);

        int priority1 = (int) (lsb >> 20) & MASK_2_BITS;
        int priority2 = (int) (lsb >> 42) & MASK_2_BITS;
        SlsVersionType type = typeFromPriority(priority1, priority2);

        int rcNumber = (int) (lsb >> 22) & MASK_20_BITS;
        int distanceFromVersion = (int) lsb & MASK_20_BITS;

        OptionalInt firstSeq = OptionalInt.empty();
        OptionalInt secondSeq = OptionalInt.empty();
        switch (type) {
            case RELEASE_CANDIDATE:
                firstSeq = OptionalInt.of(rcNumber);
                break;
            case RELEASE_SNAPSHOT:
                firstSeq = OptionalInt.of(distanceFromVersion);
                break;
            case RELEASE_CANDIDATE_SNAPSHOT:
                firstSeq = OptionalInt.of(rcNumber);
                secondSeq = OptionalInt.of(distanceFromVersion);
                break;
            case RELEASE:
            case NON_ORDERABLE:
                break;
        }
        return new OrderableSlsVersion.Builder()
                .majorVersionNumber(majorVersionNumber)
                .minorVersionNumber(minorVersionNumber)
                .patchVersionNumber(patchVersionNumber)
                .type(type)
                .firstSequenceVersionNumber(firstSeq)
                .secondSequenceVersionNumber(secondSeq)
                .value(generateVersionString(majorVersionNumber, minorVersionNumber, patchVersionNumber, type,
                        rcNumber, distanceFromVersion))
                .build();
    }

    private static SlsVersionType typeFromPriority(int priority1, int priority2) {
        if (priority2 == 2) {
            return SlsVersionType.RELEASE_SNAPSHOT;
        } else if (priority2 == 1) {
            return SlsVersionType.RELEASE;
        } else if (priority1 == 0) {
            return SlsVersionType.RELEASE_CANDIDATE;
        } else {
            return SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT;
        }
    }

    private static String generateVersionString(int major, int minor, int patch, SlsVersionType type,
            int rcNumber, int distanceFromVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(patch);
        if (type.isReleaseCandidate()) {
            sb.append("-rc").append(rcNumber);
        }
        if (type.isSnapshot()) {
            sb.append("-").append(distanceFromVersion).append("-gaaaaaa");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CompactVersion)) {
            return false;
        }
        CompactVersion other = (CompactVersion) obj;
        return msb == other.msb && lsb == other.lsb;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(msb) + Long.hashCode(lsb);
    }

    @Override
    public int compareTo(CompactVersion other) {
        if (this.msb == other.msb) {
            if (this.lsb == other.lsb) {
                return 0;
            }
            return this.lsb < other.lsb ? -1 : 1;
        }
        return this.msb < other.msb ? -1 : 1;
    }
}

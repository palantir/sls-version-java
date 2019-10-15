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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.Test;

public class OrderableSlsVersionTests {

    private static final String[] ORDERABLE_VERSIONS_IN_ORDER = new String[] {
            "0.0.0",
            "00.00.01",
            "0.1.0",
            "0.1.1",
            "1.2.0",
            "1.2.3-rc1",
            "1.2.3-rc1-4-ga",
            "1.2.3-rc2",
            "1.2.3-rc2-1-ga",
            "1.2.3-rc2-3-gb",
            "1.2.3",
            "1.2.3-9-gb",
            "1.2.3-10-ga",
            "1.2.4",
            "1.3.5",
            "1.4.0",
            "9.9.9",
            "10.0.0",
            "10.0.0-1-gaaaaaa",
            "10.0.0-2-ga"
    };

    private static final String[] ILLEGAL_VERSIONS = new String[] {
            "",
            "1.0",
            "1.z.9",
            "1.0.0.1",
            "1.0.0-FOO"
    };

    @Test
    public void testCanCreateValidVersions() {
        for (String v : ORDERABLE_VERSIONS_IN_ORDER) {
            OrderableSlsVersion.valueOf(v);
        }
    }

    @Test
    public void testCannotCreateInvalidVersions() {
        for (String v : ILLEGAL_VERSIONS) {
            assertThatThrownBy(() -> OrderableSlsVersion.valueOf(v))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void testToStringYieldsOriginalStrings() {
        for (String v : ORDERABLE_VERSIONS_IN_ORDER) {
            assertThat(OrderableSlsVersion.valueOf(v).toString()).isEqualTo(v);
        }
    }

    @Test
    public void testSerialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        for (String versionString : ORDERABLE_VERSIONS_IN_ORDER) {
            OrderableSlsVersion version = OrderableSlsVersion.valueOf(versionString);
            String serialized = mapper.writeValueAsString(version);
            assertThat(serialized).isEqualTo("\"" + versionString + "\"");

            OrderableSlsVersion deserialized = mapper.readValue(serialized, OrderableSlsVersion.class);
            assertThat(deserialized).isEqualTo(version);
        }
    }

    @Test
    public void testParsesStructureCorrectly() {
        assertThat(OrderableSlsVersion.valueOf("1.2.3"))
                .isEqualToComparingFieldByField(version("1.2.3", 1, 2, 3, SlsVersionType.RELEASE, null, null));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-rc4"))
                .isEqualToComparingFieldByField(
                        version("1.2.3-rc4", 1, 2, 3, SlsVersionType.RELEASE_CANDIDATE, 4, null));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-rc2-1-gabc"))
                .isEqualToComparingFieldByField(
                        version("1.2.3-rc2-1-gabc", 1, 2, 3, SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT, 2, 1));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-4-gabc"))
                .isEqualToComparingFieldByField(
                        version("1.2.3-4-gabc", 1, 2, 3, SlsVersionType.RELEASE_SNAPSHOT, 4, null));
    }

    @Test
    public void testVersionIsEqualToItself() {
        for (String v : ORDERABLE_VERSIONS_IN_ORDER) {
            assertThat(OrderableSlsVersion.valueOf(v)).isEqualTo(OrderableSlsVersion.valueOf(v));
            assertThat(compare(OrderableSlsVersion.valueOf(v), OrderableSlsVersion.valueOf(v))).isZero();
        }
    }

    @Test
    public void testVersionOrdering() {
        for (int i = 0; i < ORDERABLE_VERSIONS_IN_ORDER.length - 1; ++i) {
            String left = ORDERABLE_VERSIONS_IN_ORDER[i];
            String right = ORDERABLE_VERSIONS_IN_ORDER[i + 1];
            assertThat(compare(OrderableSlsVersion.valueOf(left), OrderableSlsVersion.valueOf(right))).isEqualTo(-1);
            assertThat(compare(OrderableSlsVersion.valueOf(right), OrderableSlsVersion.valueOf(left))).isEqualTo(1);
        }
    }

    @Test
    public void compareToFunctionWordsForOrderableVersions() {
        List<OrderableSlsVersion> orderedVersions = Arrays.stream(ORDERABLE_VERSIONS_IN_ORDER)
                .map(v -> OrderableSlsVersion.valueOf(v))
                .collect(Collectors.toList());
        List<OrderableSlsVersion> shuffledVersions = new ArrayList<>(orderedVersions);
        Collections.shuffle(shuffledVersions);
        Collections.sort(shuffledVersions, VersionComparator.INSTANCE);

        assertThat(shuffledVersions).isEqualTo(orderedVersions);
    }

    @Test
    public void testTargetedVersionOrdering() {
        // Snapshots vs snapshots.
        assertVersionsInOrder("1.0.0-2-ga", "1.0.1-1-gb");
        assertVersionsInOrder("1.0.0-2-ga", "1.0.0-3-gb");

        // Releases vs releases.
        assertVersionsInOrder("0.0.1", "0.0.2");
        assertVersionsInOrder("0.1.0", "0.2.0");
        assertVersionsInOrder("0.1.2", "0.2.0");
        assertVersionsInOrder("0.1.2", "1.0.0");

        // Rcs vs rcs.
        assertVersionsInOrder("1.0.0-rc2", "1.0.0-rc3");
        assertVersionsInOrder("1.0.0-rc2", "1.0.1-rc1");

        // Rc snapshots vs rc snapshots
        assertVersionsInOrder("1.0.0-rc2-2-ga", "1.0.0-rc2-3-gb");
        assertVersionsInOrder("1.0.0-rc2-3-ga", "1.0.1-rc1-1-gb");
        assertVersionsInOrder("1.0.1-rc1-3-ga", "1.0.1-rc2-1-gb");

        // Releases vs snapshots.
        assertVersionsInOrder("1.0.0", "1.0.0-2-ga");
        assertVersionsInOrder("1.0.0-2-ga", "1.0.1");

        // Releases vs rcs.
        assertVersionsInOrder("1.0.0-rc1", "1.0.0");
        assertVersionsInOrder("1.0.0", "1.0.1-rc1");

        // Releases vs rc snapshots.
        assertVersionsInOrder("1.0.0", "1.0.1-rc1-2-ga");
        assertVersionsInOrder("1.0.0-rc3-2-ga", "1.0.1");

        // Snapshots vs rcs.
        assertVersionsInOrder("1.0.0-rc5", "1.0.0-2-ga");
        assertVersionsInOrder("1.0.0-5-ga", "1.0.1-rc1");

        // Snapshots vs rc snapshots.
        assertVersionsInOrder("1.0.0-rc5-2-ga", "1.0.0-2-ga");
        assertVersionsInOrder("1.0.0-5-ga", "1.0.1-rc1-2-ga");

        // Rcs vs rc snapshots.
        assertVersionsInOrder("1.0.0-rc1", "1.0.0-rc1-1-ga");
        assertVersionsInOrder("1.0.0-rc1-5-ga", "1.0.0-rc5");
        assertVersionsInOrder("1.0.0-rc2-3-ga", "1.0.1-rc1");
        assertVersionsInOrder("1.0.0-rc2", "1.0.1-rc1-3-ga");
    }

    @Test
    public void testEqualVersions() {
        assertVersionsEqual("1.0.0", "1.0.0");
        assertVersionsEqual("1.0.0-2-gaaaaa", "1.0.0-2-gbbbbb");
        assertVersionsEqual("1.0.0-rc3", "1.0.0-rc3");
        assertVersionsEqual("1.0.0-rc3-4-gaaaaa", "1.0.0-rc3-4-gbbbbbb");
    }

    private void assertVersionsInOrder(String smaller, String larger) {
        assertThat(compare(OrderableSlsVersion.valueOf(smaller), OrderableSlsVersion.valueOf(larger))).isEqualTo(-1);
    }

    private void assertVersionsEqual(String left, String right) {
        assertThat(compare(OrderableSlsVersion.valueOf(left), OrderableSlsVersion.valueOf(right))).isZero();
    }

    private OrderableSlsVersion version(
            String version,
            int major,
            int minor,
            int patch,
            SlsVersionType type,
            Integer firstSequence,
            Integer secondSequence) {
        return new OrderableSlsVersion.Builder()
                .value(version)
                .majorVersionNumber(major)
                .minorVersionNumber(minor)
                .patchVersionNumber(patch)
                .type(type)
                .firstSequenceVersionNumber(firstSequence == null ? OptionalInt.empty() : OptionalInt.of(firstSequence))
                .secondSequenceVersionNumber(
                        secondSequence == null ? OptionalInt.empty() : OptionalInt.of(secondSequence))
                .build();
    }

    private int compare(OrderableSlsVersion left, OrderableSlsVersion right) {
        return VersionComparator.INSTANCE.compare(left, right);
    }
}

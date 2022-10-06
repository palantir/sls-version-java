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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public final class OrderableSlsVersionTests {

    static List<String> orderableVersionsInOrder() {
        return ImmutableList.of(
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
                "10.0.0-2-ga");
    }

    @ParameterizedTest
    @MethodSource("orderableVersionsInOrder")
    public void testCanCreateValidVersions(String value) {
        assertThatCode(() -> OrderableSlsVersion.valueOf(value)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1.0", "1.z.9", "1.0.0.1", "1.0.0-FOO"})
    public void testCannotCreateInvalidVersions(String illegalVersion) {
        assertThatThrownBy(() -> OrderableSlsVersion.valueOf(illegalVersion))
                .isInstanceOf(SafeIllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("orderableVersionsInOrder")
    public void testToStringYieldsOriginalStrings(String value) {
        assertThat(OrderableSlsVersion.valueOf(value).toString()).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("orderableVersionsInOrder")
    public void testSerialization(String versionString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(OrderableSlsVersion.class);
        ObjectWriter writer = mapper.writerFor(OrderableSlsVersion.class);

        OrderableSlsVersion version = OrderableSlsVersion.valueOf(versionString);

        String serialized = mapper.writeValueAsString(version);
        assertThat(serialized).isEqualTo("\"" + versionString + "\"");
        assertThat(serialized).isEqualTo(writer.writeValueAsString(version));

        SlsVersion deserialized = mapper.readValue(serialized, SlsVersion.class);
        assertThat(deserialized).isEqualTo(version);
        assertThat(deserialized).isEqualTo(reader.readValue(serialized, SlsVersion.class));
    }

    @Test
    public void testParsesStructureCorrectly() {
        assertThat(OrderableSlsVersion.valueOf("1.2.3"))
                .usingRecursiveComparison()
                .isEqualTo(version("1.2.3", 1, 2, 3, SlsVersionType.RELEASE, null, null));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-rc4"))
                .usingRecursiveComparison()
                .isEqualTo(version("1.2.3-rc4", 1, 2, 3, SlsVersionType.RELEASE_CANDIDATE, 4, null));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-rc2-1-gabc"))
                .usingRecursiveComparison()
                .isEqualTo(version("1.2.3-rc2-1-gabc", 1, 2, 3, SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT, 2, 1));
        assertThat(OrderableSlsVersion.valueOf("1.2.3-4-gabc"))
                .usingRecursiveComparison()
                .isEqualTo(version("1.2.3-4-gabc", 1, 2, 3, SlsVersionType.RELEASE_SNAPSHOT, 4, null));
    }

    @ParameterizedTest
    @MethodSource("orderableVersionsInOrder")
    public void testVersionIsEqualToItself(String value) {
        assertThat(OrderableSlsVersion.valueOf(value))
                .isEqualTo(OrderableSlsVersion.valueOf(value))
                .isEqualByComparingTo(OrderableSlsVersion.valueOf(value));
    }

    @Test
    public void testVersionOrdering() {
        for (int i = 0; i < orderableVersionsInOrder().size() - 1; ++i) {
            String left = orderableVersionsInOrder().get(i);
            String right = orderableVersionsInOrder().get(i + 1);
            assertThat(OrderableSlsVersion.valueOf(left)).isLessThan(OrderableSlsVersion.valueOf(right));
            assertThat(OrderableSlsVersion.valueOf(right)).isGreaterThan(OrderableSlsVersion.valueOf(left));
        }
    }

    @Test
    public void compareToFunctionWordsForOrderableVersions() {
        List<OrderableSlsVersion> orderedVersions = orderableVersionsInOrder().stream()
                .map(OrderableSlsVersion::valueOf)
                .collect(Collectors.toList());
        List<OrderableSlsVersion> shuffledVersions = new ArrayList<>(orderedVersions);
        Collections.shuffle(shuffledVersions);
        Collections.sort(shuffledVersions, VersionComparator.INSTANCE);

        assertThat(shuffledVersions).containsExactlyElementsOf(orderedVersions);
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
    public void testCheckWithOrderableVersion() {
        assertThat(OrderableSlsVersion.check("1.0.0")).isTrue();
    }

    @Test
    public void testCheckWithNonOrderableVersion() {
        assertThat(OrderableSlsVersion.check("1.0.0-foo")).isFalse();
    }

    @Test
    public void testCheckWithGarbage() {
        assertThat(OrderableSlsVersion.check("foo")).isFalse();
    }

    @Test
    public void testEqualVersions() {
        assertVersionsEqual("1.0.0", "1.0.0");
        assertVersionsEqual("1.0.0-2-gaaaaa", "1.0.0-2-gbbbbb");
        assertVersionsEqual("1.0.0-rc3", "1.0.0-rc3");
        assertVersionsEqual("1.0.0-rc3-4-gaaaaa", "1.0.0-rc3-4-gbbbbbb");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "2147483648.0.0",
                "0.2147483648.0",
                "0.0.2147483648",
                "0.0.0-rc2147483648",
                "0.0.0-2147483648-gaaa"
            })
    void integer_overflow(String overflowVersion) {
        assertThatThrownBy(() -> OrderableSlsVersion.valueOf(overflowVersion))
                .satisfiesAnyOf(
                        throwable -> assertThat(throwable)
                                .hasMessage("Not an orderable version: {value}: {value=" + overflowVersion + "}"),
                        throwable -> assertThat(throwable)
                                .hasMessageContaining("" + "Can't parse segment as integer as it overflowed: {string="
                                        + overflowVersion));
    }

    @Test
    void leading_zeros() {
        assertThat(Stream.of("9.9.10", "9.9.2", "9.9.02", "9.9.1", "9.9.01")
                        .map(OrderableSlsVersion::valueOf)
                        .sorted()
                        .map(OrderableSlsVersion::toString))
                .describedAs("This sorting behaviour might look odd, but it's because the patch segment "
                        + "is interpreted as an integer, and 01 and 1 are the same.")
                .containsExactly("9.9.1", "9.9.01", "9.9.2", "9.9.02", "9.9.10");

        assertThat(Stream.of("9.9.1", "9.9.01", "9.9.2", "9.9.02", "9.9.10").sorted())
                .describedAs(
                        "Just for comparison, here's the lexicographical ordering, " + "which is totally different")
                .containsExactly("9.9.01", "9.9.02", "9.9.1", "9.9.10", "9.9.2");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0.0.0",
                "0.0.1",
                "0.1.0",
                "0.1.1",
                "1.0.0",
                "1.0.1",
                "1.1.0",
                "1.1.1",
                "1.1.10",
                "1.2.3-rc4",
            })
    void intern_normalized_versions(String value) {
        assertThat(OrderableSlsVersion.valueOf(value))
                .isEqualTo(OrderableSlsVersion.valueOf(value))
                .isEqualByComparingTo(OrderableSlsVersion.valueOf(value))
                .isSameAs(OrderableSlsVersion.valueOf(value));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0.0.00",
                "0.0.01",
                "0.01.0",
                "0.01.01",
                "01.0.0",
                "01.0.1",
                "01.01.0",
                "01.01.01",
                "01.01.010",
                "1.0.0-rc3-4-gaaaaa",
                "1.0.0-rc3-4-gbbbbbb",
            })
    void does_not_intern_non_normalized_versions(String value) {
        assertThat(OrderableSlsVersion.valueOf(value))
                .isEqualTo(OrderableSlsVersion.valueOf(value))
                .isEqualByComparingTo(OrderableSlsVersion.valueOf(value))
                .isNotSameAs(OrderableSlsVersion.valueOf(value));
    }

    private void assertVersionsInOrder(String smaller, String larger) {
        assertThat(OrderableSlsVersion.valueOf(smaller))
                .isLessThan(OrderableSlsVersion.valueOf(larger))
                .isNotEqualByComparingTo(OrderableSlsVersion.valueOf(larger));
        assertThat(OrderableSlsVersion.valueOf(larger))
                .isGreaterThanOrEqualTo(OrderableSlsVersion.valueOf(smaller))
                .isNotEqualByComparingTo(OrderableSlsVersion.valueOf(smaller));
        assertThat(VersionComparator.INSTANCE.compare(
                        OrderableSlsVersion.valueOf(smaller), OrderableSlsVersion.valueOf(larger)))
                .isEqualTo(-1);
        assertThat(VersionComparator.INSTANCE.compare(
                        OrderableSlsVersion.valueOf(larger), OrderableSlsVersion.valueOf(smaller)))
                .isEqualTo(1);
    }

    private void assertVersionsEqual(String left, String right) {
        assertThat(OrderableSlsVersion.valueOf(left))
                .isEqualTo(OrderableSlsVersion.valueOf(right))
                .isEqualByComparingTo(OrderableSlsVersion.valueOf(right));
        assertThat(VersionComparator.INSTANCE.compare(
                        OrderableSlsVersion.valueOf(left), OrderableSlsVersion.valueOf(right)))
                .isZero();
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
}

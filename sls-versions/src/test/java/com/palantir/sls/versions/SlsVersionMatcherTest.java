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

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

public class SlsVersionMatcherTest {

    @Test
    public void testParsesValidMatchers() {
        assertThat(matcher("x.x.x")).isEqualTo(matcher("x.x.x", null, null, null));
        assertThat(matcher("1.x.x")).isEqualTo(matcher("1.x.x", 1, null, null));
        assertThat(matcher("1.2.x")).isEqualTo(matcher("1.2.x", 1, 2, null));
        assertThat(matcher("01.02.x")).isEqualTo(matcher("1.2.x", 1, 2, null));
        assertThat(matcher("1.2.3")).isEqualTo(matcher("1.2.3", 1, 2, 3));
    }

    @Test
    public void testDoesNotParseInvalidMatches() {
        assertThat(SlsVersionMatcher.safeValueOf("x.x.x-foo")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.x.x.x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1x.x.x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.1x.x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.x.1x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.x.x.x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.y.z")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.2.3-x")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.2.3-rcx")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.2.3-rc1")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.2.3-rc1-x-gabcde")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.2.3-x-gabcde")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.2.3")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("1.x.3")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.x.3")).isEmpty();
        assertThat(SlsVersionMatcher.safeValueOf("x.2.x")).isEmpty();
    }

    @Test
    public void testToStringYieldsOriginalValue() {
        assertThat(matcher("x.x.x")).hasToString("x.x.x");
        assertThat(matcher("1.x.x")).hasToString("1.x.x");
        assertThat(matcher("1.2.x")).hasToString("1.2.x");
        assertThat(matcher("1.2.3")).hasToString("1.2.3");
    }

    @Test
    public void testMatches() {
        assertThat(matcher("x.x.x").matches(version("0.0.0"))).isTrue();
        assertThat(matcher("x.x.x").matches(version("2.3.4"))).isTrue();
        assertThat(matcher("x.x.x").matches(version("2.3.4-5-gabcdef"))).isFalse();
        assertThat(matcher("x.x.x").matches(version("2.3.4-rc5"))).isFalse();
        assertThat(matcher("x.x.x").matches(version("2.3.4-rc3-1-gabc"))).isFalse();

        assertThat(matcher("2.x.x").matches(version("2.3.4"))).isTrue();
        assertThat(matcher("2.x.x").matches(version("2.3.4-5-gabcdef"))).isFalse();
        assertThat(matcher("2.x.x").matches(version("2.3.4-rc5"))).isFalse();
        assertThat(matcher("2.x.x").matches(version("2.3.4-rc3-1-gcba"))).isFalse();
        assertThat(matcher("2.x.x").matches(version("1.3.4"))).isFalse();
        assertThat(matcher("2.x.x").matches(version("3.3.4"))).isFalse();

        assertThat(matcher("2.3.x").matches(version("2.3.4"))).isTrue();
        assertThat(matcher("2.3.x").matches(version("2.3.4-5-gabcdef"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("2.3.4-rc5"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("2.3.4-rc3-1-gbbb"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("2.2.4"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("2.4.4"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("1.3.4"))).isFalse();
        assertThat(matcher("2.3.x").matches(version("3.3.4"))).isFalse();

        assertThat(matcher("1.2.x").matches(version("1.2.3-rc1"))).isFalse();

        assertThat(matcher("1.2.3").matches(version("1.2.3"))).isTrue();
        assertThat(matcher("1.2.3").matches(version("1.2.3-rc1"))).isFalse();
        assertThat(matcher("1.2.3").matches(version("2.3.4-rc3-1-gbbb"))).isFalse();
    }

    @Test
    public void testCompare() {
        assertThat(matcher("x.x.x").compare(version("0.0.0"))).isZero();

        assertThat(matcher("1.x.x").compare(version("0.0.0"))).isPositive();
        assertThat(matcher("1.x.x").compare(version("1.0.0"))).isZero();
        assertThat(matcher("1.x.x").compare(version("2.0.0"))).isNegative();

        assertThat(matcher("1.2.x").compare(version("1.1.0"))).isPositive();
        assertThat(matcher("1.2.x").compare(version("1.2.3"))).isZero();
        assertThat(matcher("1.2.x").compare(version("1.3.2"))).isNegative();

        assertThat(matcher("1.2.3").compare(version("1.2.4"))).isNegative();
        assertThat(matcher("1.2.3").compare(version("1.2.3-rc1"))).isPositive();

        assertThat(matcher("1.2.x").compare(version("1.2.3-rc1"))).isZero();
    }

    @Test
    public void testMatcherComparator_xTrumpsNumber() {
        assertMatcherOrder(SlsVersionMatcher.valueOf("2.6.x"), SlsVersionMatcher.valueOf("2.x.x"));
        assertMatcherOrder(SlsVersionMatcher.valueOf("2.6.5"), SlsVersionMatcher.valueOf("2.6.x"));
    }

    @Test
    public void testMatcherComparator_comparesNumbers() {
        assertMatcherOrder(SlsVersionMatcher.valueOf("2.6.x"), SlsVersionMatcher.valueOf("2.7.x"));
        assertMatcherOrder(SlsVersionMatcher.valueOf("2.6.5"), SlsVersionMatcher.valueOf("2.6.6"));
    }

    private static void assertMatcherOrder(SlsVersionMatcher smaller, SlsVersionMatcher larger) {
        assertThat(SlsVersionMatcher.MATCHER_COMPARATOR.compare(smaller, larger))
                .isLessThan(0);
        assertThat(SlsVersionMatcher.MATCHER_COMPARATOR.compare(larger, smaller))
                .isGreaterThan(0);
    }

    private static SlsVersionMatcher matcher(String value) {
        return SlsVersionMatcher.valueOf(value);
    }

    private SlsVersionMatcher matcher(String value, Integer major, Integer minor, Integer patch) {
        return new SlsVersionMatcher.Builder()
                .value(value)
                .majorVersionNumber(major == null ? OptionalInt.empty() : OptionalInt.of(major))
                .minorVersionNumber(minor == null ? OptionalInt.empty() : OptionalInt.of(minor))
                .patchVersionNumber(patch == null ? OptionalInt.empty() : OptionalInt.of(patch))
                .build();
    }

    private static OrderableSlsVersion version(String version) {
        return OrderableSlsVersion.valueOf(version);
    }
}

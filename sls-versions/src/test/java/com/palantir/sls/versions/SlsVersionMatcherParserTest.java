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

import static org.assertj.core.api.Assertions.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.constant;
import static org.quicktheories.generators.Generate.frequency;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.lists;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Pair;
import org.quicktheories.core.Gen;

/**
 * This test uses the <a href="https://github.com/quicktheories/QuickTheories">QuickTheories library</a>
 * to generate a whole bunch of random test cases, and validate that the {@link SlsVersionMatcherParser} behaves
 * identically to the reference {@link RegexSlsVersionMatcherParser}.
 *
 * It's effectively a java version of Haskell's QuickCheck library, and has cool features like 'shrinking', whereby
 * it will try to present the smallest possible repro of any failure rather than the obscure long version it may have
 * discovered first.
 */
public final class SlsVersionMatcherParserTest {

    private static Gen<String> validComponent() {
        return frequency(
                Pair.of(99, integers().allPositive().map(i -> Integer.toString(i))), Pair.of(1, constant("x")));
    }

    @Test
    public void valid_parsing() {
        qt().withExamples(2000)
                .forAll(validComponent(), validComponent(), validComponent())
                .checkAssert((major, minor, patch) -> {
                    String string = major + "." + minor + "." + patch;
                    assertThat(SlsVersionMatcherParser.safeValueOf(string))
                            .describedAs(string)
                            .isEqualTo(RegexSlsVersionMatcherParser.safeValueOf(string));
                });
    }

    private static final List<Character> NONSENSE_CHARS =
            Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'x', '.');

    @Test
    public void nonsense_parsing() {
        Gen<String> nonsense = lists().of(pick(NONSENSE_CHARS))
                .ofSizeBetween(0, 10)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder(chars.size());
                    chars.forEach(sb::append);
                    return sb.toString();
                });

        qt().withExamples(2000).forAll(nonsense).checkAssert(str -> {
            assertThat(SlsVersionMatcherParser.safeValueOf(str))
                    .describedAs(str)
                    .isEqualTo(RegexSlsVersionMatcherParser.safeValueOf(str));
        });
    }
}

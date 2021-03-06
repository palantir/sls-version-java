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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Chars;
import net.jqwik.api.constraints.NumericChars;

/**
 * This test uses the <a href="https://jqwik.net/">jqwik library</a>
 * to generate a whole bunch of random test cases, and validate that the {@link SlsVersionMatcherParser} behaves
 * identically to the reference {@link RegexSlsVersionMatcherParser}.
 *
 * It's effectively a java version of Haskell's QuickCheck library, and has cool features like 'shrinking', whereby
 * it will try to present the smallest possible repro of any failure rather than the obscure long version it may have
 * discovered first.
 */
public final class SlsVersionMatcherParserTest {

    @Property(seed = "3226259347315412165", tries = 2000)
    public void valid_parsing(
            @ForAll("validComponent") String major,
            @ForAll("validComponent") String minor,
            @ForAll("validComponent") String patch) {
        String string = major + '.' + minor + '.' + patch;
        assertThat(SlsVersionMatcherParser.safeValueOf(string))
                .describedAs(string)
                .isEqualTo(RegexSlsVersionMatcherParser.safeValueOf(string));
    }

    @Provide
    public Arbitrary<String> validComponent() {
        Arbitrary<String> justX = Arbitraries.just("x");
        Arbitrary<String> integerArbitrary =
                Arbitraries.integers().greaterOrEqual(0).map(a -> Integer.toString(a));
        return Arbitraries.oneOf(justX, integerArbitrary);
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @NumericChars
    @Chars({'x', '.'})
    public @interface Nonsense {}

    @Property(seed = "3226259347315412165", tries = 2000)
    public void nonsense_parsing(@ForAll @Nonsense String nonsense) {
        assertThat(SlsVersionMatcherParser.safeValueOf(nonsense))
                .describedAs(nonsense)
                .isEqualTo(RegexSlsVersionMatcherParser.safeValueOf(nonsense));
    }
}

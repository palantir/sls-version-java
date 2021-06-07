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
import static com.palantir.logsafe.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.UnsafeArg;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An SLS version matcher as defined by the [SLS
 * spec](https://github.com/palantir/sls-version-java#sls-product-version-specification).
 */
@Value.Immutable
@ImmutablesStyle
public abstract class SlsVersionMatcher {

    private static final Logger log = LoggerFactory.getLogger(SlsVersionMatcher.class);
    private static final Pattern PATTERN = Pattern.compile("^(([0-9]+|x))\\.(([0-9]+|x))\\.(([0-9]+|x))$");

    private static final Comparator<OptionalInt> EMPTY_IS_GREATER =
            Comparator.comparingInt(num -> num.isPresent() ? num.getAsInt() : Integer.MAX_VALUE);

    public static final Comparator<SlsVersionMatcher> MATCHER_COMPARATOR = Comparator.comparing(
                    SlsVersionMatcher::getMajorVersionNumber, EMPTY_IS_GREATER)
            .thenComparing(SlsVersionMatcher::getMinorVersionNumber, EMPTY_IS_GREATER)
            .thenComparing(SlsVersionMatcher::getPatchVersionNumber, EMPTY_IS_GREATER);

    @Value.Auxiliary
    public abstract String getValue();

    public abstract OptionalInt getMajorVersionNumber();

    public abstract OptionalInt getMinorVersionNumber();

    public abstract OptionalInt getPatchVersionNumber();

    @JsonCreator
    public static SlsVersionMatcher valueOf(String value) {
        Optional<SlsVersionMatcher> optional = safeValueOf(value);
        checkArgument(optional.isPresent(), "Not a valid SLS version matcher: {value}", UnsafeArg.of("value", value));
        return optional.get();
    }

    /** The same as {@link #valueOf(String)}, but returns {@link Optional#empty} if the format is invalid. */
    public static Optional<SlsVersionMatcher> safeValueOf(String value) {
        checkNotNull(value, "value cannot be null");

        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        } else {
            SlsVersionMatcher maybeMatcher = new SlsVersionMatcher.Builder()
                    .value(value)
                    .majorVersionNumber(parseInt(matcher.group(1)))
                    .minorVersionNumber(parseInt(matcher.group(3)))
                    .patchVersionNumber(parseInt(matcher.group(5)))
                    .build();

            if (maybeMatcher.getPatchVersionNumber().isPresent()
                    && (!maybeMatcher.getMinorVersionNumber().isPresent()
                            || !maybeMatcher.getMajorVersionNumber().isPresent())) {
                // String contains a pattern where major or minor version is underspecified.
                // Example: x.x.2, 1.x.3, x.2.3
                log.info(
                        "Not a valid matcher, a patch version is specified, yet a major or minor is not specified",
                        SafeArg.of("matcher", maybeMatcher));
                return Optional.empty();
            }
            if (maybeMatcher.getMinorVersionNumber().isPresent()
                    && !maybeMatcher.getMajorVersionNumber().isPresent()) {
                // String contains a pattern where major version is underspecified. Example: x.2.x
                log.info(
                        "Not a valid matcher, a minor version is specified, yet a major is not specified",
                        SafeArg.of("matcher", maybeMatcher));
                return Optional.empty();
            }
            return Optional.of(maybeMatcher);
        }
    }

    /**
     * Returns true iff {@link #compare(OrderableSlsVersion)} returns 0.
     *
     * @see #compare(OrderableSlsVersion)
     */
    public boolean matches(OrderableSlsVersion version) {
        // a version matcher never matches a non-release version
        if (version.getType() != SlsVersionType.RELEASE) {
            return false;
        }

        return compare(version) == 0;
    }

    /**
     * Returns 0 iff the given version matches this matcher, i.e., if the non-{@code x} major/minor/patch versions of
     * this matcher are identical to the respective major/minor/patch versions of the given version. Note that despite
     * the syntactic similarity between matchers and {@link SlsVersionType#RELEASE release versions}, a matcher can
     * match release snapshot, release, release-candidate, and release-candidate snapshot versions. For example, matcher
     * {@code 1.x.x} matches versions {@code 1.0.0} and {@code 1.2.3-rc4}, but it does not match versions {@code 2.0.0}
     * or {@code 0.1.0-rc4}.
     *
     * <p>Returns a negative number if any existing component is less than the given version and a positive number if
     * any existing component is greater than the given version. For example matcher {@code 1.2.x} is less than
     * {@code 1.3.2} and {@code 1.2.x} is greater than {@code 1.1.1}.
     */
    public int compare(OrderableSlsVersion version) {
        if (concreteSlsVersion().isPresent()) {
            return VersionComparator.INSTANCE.compare(concreteSlsVersion().get(), version);
        }

        if (getMajorVersionNumber().isPresent()) {
            int comparison = Integer.compare(getMajorVersionNumber().getAsInt(), version.getMajorVersionNumber());
            if (comparison != 0) {
                return comparison;
            }
        }
        if (getMinorVersionNumber().isPresent()) {
            int comparison = Integer.compare(getMinorVersionNumber().getAsInt(), version.getMinorVersionNumber());
            if (comparison != 0) {
                return comparison;
            }
        }
        if (getPatchVersionNumber().isPresent()) {
            int comparison = Integer.compare(getPatchVersionNumber().getAsInt(), version.getPatchVersionNumber());
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    /**
     * Returns an OrderableSlsVersion if this VersionMatcher is a concrete version (that is to say, it contains no x's).
     */
    @Value.Lazy
    protected Optional<OrderableSlsVersion> concreteSlsVersion() {
        return OrderableSlsVersion.safeValueOf(getValue());
    }

    private static OptionalInt parseInt(String maybeInt) {
        if (maybeInt.length() == 1) {
            return parseIntFromChar(maybeInt.charAt(0));
        }

        try {
            return OptionalInt.of(Integer.parseInt(maybeInt));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private static OptionalInt parseIntFromChar(char onlyChar) {
        if (onlyChar == 'x') {
            return OptionalInt.empty();
        } else if (Character.isDigit(onlyChar)) {
            return OptionalInt.of(Character.getNumericValue(onlyChar));
        } else {
            return OptionalInt.empty();
        }
    }

    @JsonValue
    @Override
    public final String toString() {
        return getValue();
    }

    public static final class Builder extends ImmutableSlsVersionMatcher.Builder {}
}

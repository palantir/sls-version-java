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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public final class CompactVersionTests {

    private static final List<OrderableSlsVersion> versions = Arrays.asList(
            "0.0.0-rc0",
            "0.0.0-rc0-1-gbbb",
            "0.0.0",
            "0.0.0-1-gbbb",
            "0.0.1",
            "0.1.0",
            "0.1.1",
            "1.0.0-rc0",
            "1.0.0-rc0-2-gbbb",
            "1.0.0-rc1",
            "1.0.0-rc1-1-gbbb",
            "1.0.0",
            "1.0.0-1-gbbb",
            "1.0.0-2-gbbb",
            "1.0.1",
            "1.1.1",
            "1048575.1048575.1048575-rc1048575-1048575-gbbb")
            .stream()
            .map(OrderableSlsVersion::valueOf)
            .collect(Collectors.toList());

    @Test
    public void testCompactVersionRoundTrips() {
        for (OrderableSlsVersion version : versions) {
            assertThat(CompactVersion.from(version).toSlsVersion()).isEqualTo(version);
        }
    }

    @Test
    public void testCompactVersionSortOrder() {
        for (int i = 0; i < versions.size() - 1; i++) {
            assertThat(CompactVersion.from(versions.get(i))).isLessThan(CompactVersion.from(versions.get(i + 1)));
            assertThat(CompactVersion.from(versions.get(i + 1))).isGreaterThan(CompactVersion.from(versions.get(i)));
            assertThat(CompactVersion.from(versions.get(i))).isEqualTo(CompactVersion.from(versions.get(i)));
        }
    }

    @Test
    public void testByteValuesMatchSortOrder() {
        for (int i = 0; i < versions.size() - 1; i++) {
            CompactVersion left = CompactVersion.from(versions.get(i));
            CompactVersion right = CompactVersion.from(versions.get(i + 1));
            assertThat(compare(left.getMsb(), left.getLsb(), right.getMsb(), right.getLsb())).isNegative();
            assertThat(compare(right.getMsb(), right.getLsb(), left.getMsb(), left.getLsb())).isPositive();
            assertThat(compare(left.getMsb(), left.getLsb(), left.getMsb(), left.getLsb())).isZero();
        }
    }

    private static int compare(long msbLeft, long lsbLeft, long msbRight, long lsbRight) {
        if (msbLeft == msbRight) {
            if (lsbLeft == lsbRight) {
                return 0;
            }
            return lsbLeft < lsbRight ? -1 : 1;
        }
        return msbLeft < msbRight ? -1 : 1;
    }

}

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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

public class SlsVersionTests {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testCanCreateOrderableVersions() throws IOException {
        String versionString = "1.0.0";
        SlsVersion version = SlsVersion.valueOf(versionString);
        assertThat(version).isInstanceOf(OrderableSlsVersion.class);
        String serialized = mapper.writeValueAsString(version);
        assertThat(mapper.readValue(serialized, SlsVersion.class)).isEqualTo(version);
    }

    @Test
    public void testCanCreateNonOrderableVersions() throws IOException {
        String versionString = "1.0.0-foo";
        SlsVersion version = SlsVersion.valueOf(versionString);
        assertThat(version).isInstanceOf(NonOrderableSlsVersion.class);

        String serialized = mapper.writeValueAsString(version);
        assertThat(mapper.readValue(serialized, SlsVersion.class)).isEqualTo(version);
    }

    @Test
    public void testCheckWithOrderableVersion() {
        assertThat(SlsVersion.check("1.0.0")).isTrue();
    }

    @Test
    public void testCheckWithNonOrderableVersion() {
        assertThat(SlsVersion.check("1.0.0-foo")).isTrue();
    }

    @Test
    public void testCheckWithGarbage() {
        assertThat(SlsVersion.check("foo")).isFalse();
    }
}

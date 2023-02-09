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
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class NonOrderableVersionTests {
    private static final String[] VALID_NONORDERABLE_VERSIONS =
            new String[] {"1.0.0.dirty", "0.0.1-custom-description-42", "2.0.0-1-gaaaaaa.dirty"};

    private static final String[] ILLEGAL_VERSIONS = new String[] {"5.0", "1.1.2.3-foo", "1.1.2.3", "1.0.0-FOO"};

    @Test
    public void testCanCreateValidVersions() {
        for (String v : VALID_NONORDERABLE_VERSIONS) {
            NonOrderableSlsVersion.valueOf(v);
        }
    }

    @Test
    public void testCannotCreateInvalidVersions() {
        for (String v : ILLEGAL_VERSIONS) {
            assertThatThrownBy(() -> NonOrderableSlsVersion.valueOf(v))
                    .isInstanceOf(SafeIllegalArgumentException.class);
        }
    }

    @Test
    public void testPatchVersionParsing() {
        SlsVersion version = NonOrderableSlsVersion.valueOf("1.2.3-blah");
        assertThat(version.getMajorVersionNumber()).isEqualTo(1);
        assertThat(version.getMinorVersionNumber()).isEqualTo(2);
        assertThat(version.getPatchVersionNumber()).isEqualTo(3);
        assertThat(version.getType()).isEqualTo(SlsVersionType.NON_ORDERABLE);
    }

    @Test
    public void testIncorrectPatchVersionParsing() {
        assertThatThrownBy(() -> NonOrderableSlsVersion.valueOf("1.2.foo"))
                .isInstanceOf(SafeIllegalArgumentException.class);
    }

    @Test
    public void testToStringYieldsOriginalStrings() {
        assertThat(NonOrderableSlsVersion.valueOf("1.2.3-foo").toString()).isEqualTo("1.2.3-foo");
    }

    @Test
    public void testSerialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(NonOrderableSlsVersion.class);
        ObjectWriter writer = mapper.writerFor(NonOrderableSlsVersion.class);

        String versionString = "1.2.3-foo";
        NonOrderableSlsVersion version = NonOrderableSlsVersion.valueOf(versionString);

        String serialized = mapper.writeValueAsString(version);
        assertThat(serialized).isEqualTo("\"" + versionString + "\"");
        assertThat(serialized).isEqualTo(writer.writeValueAsString(version));

        SlsVersion deserialized = mapper.readValue(serialized, SlsVersion.class);
        assertThat(deserialized).isEqualTo(version);
        assertThat(deserialized).isEqualTo(reader.readValue(serialized, SlsVersion.class));
    }

    @Test
    public void testCheckWithOrderableVersion() {
        assertThat(NonOrderableSlsVersion.check("1.0.0")).isFalse();
    }

    @Test
    public void testCheckWithNonOrderableVersion() {
        assertThat(NonOrderableSlsVersion.check("1.0.0-foo")).isTrue();
    }

    @Test
    public void testCheckWithGarbage() {
        assertThat(NonOrderableSlsVersion.check("foo")).isFalse();
    }
}

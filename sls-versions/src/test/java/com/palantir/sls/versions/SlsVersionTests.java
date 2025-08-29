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
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SlsVersionTests {

    private static final Path SERIALIZABLE_CASES_PATH = Path.of("src/test/serializableCases");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectReader READER = MAPPER.readerFor(SlsVersion.class);
    private static final ObjectWriter WRITER = MAPPER.writerFor(SlsVersion.class);

    @Test
    public void testCanCreateOrderableVersions() throws IOException {
        String versionString = "1.0.0";
        SlsVersion version = SlsVersion.valueOf(versionString);

        assertThat(version).isInstanceOf(OrderableSlsVersion.class);

        String serialized = MAPPER.writeValueAsString(version);
        assertThat(serialized).isEqualTo("\"" + versionString + "\"");
        assertThat(serialized).isEqualTo(WRITER.writeValueAsString(version));

        SlsVersion deserialized = MAPPER.readValue(serialized, SlsVersion.class);
        assertThat(deserialized).isEqualTo(version);
        assertThat(deserialized).isEqualTo(READER.readValue(serialized, SlsVersion.class));
    }

    @Test
    public void testCanCreateNonOrderableVersions() throws IOException {
        String versionString = "1.0.0-foo";
        SlsVersion version = SlsVersion.valueOf(versionString);

        assertThat(version).isInstanceOf(NonOrderableSlsVersion.class);

        String serialized = MAPPER.writeValueAsString(version);
        assertThat(serialized).isEqualTo("\"" + versionString + "\"");
        assertThat(serialized).isEqualTo(WRITER.writeValueAsString(version));

        SlsVersion deserialized = MAPPER.readValue(serialized, SlsVersion.class);
        assertThat(deserialized).isEqualTo(version);
        assertThat(deserialized).isEqualTo(READER.readValue(serialized, SlsVersion.class));
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

    @ParameterizedTest
    @MethodSource("serializable_arguments")
    public void serializable(int version, SlsVersion input, SlsVersion expected) throws Exception {
        Path path =
                SERIALIZABLE_CASES_PATH.resolve(String.format("v" + version)).resolve(input.getValue() + ".ser");

        if (!Files.exists(path) && Boolean.getBoolean("recreate")) {
            writeObject(path, input);
        }

        SlsVersion actual = readObject(path, SlsVersion.class);

        assertThat(actual).isEqualTo(expected);
    }

    // The version number here is intended to represent a specific version of this library. The
    // version number is used in the expected file path in order to be able to test the serialization behavior of the
    // same value in different versions of this library.
    //
    // The version number and expected file for existing test cases should NEVER be changed. The expected value for
    // existing test cases can be changed if the SlsVersion API changes (but the existing serialized values still need
    // to deserialize "correctly").
    //
    // When changing to the serialization behehavior and adding new test cases, the new test cases should always use a
    // new version number.
    private static List<Arguments> serializable_arguments() {
        return List.of(
                Arguments.of(
                        0,
                        SlsVersion.valueOf("1.2.3-4-gabcdef"),
                        new OrderableSlsVersion.Builder()
                                .value("1.2.3-4-gabcdef")
                                .majorVersionNumber(1)
                                .minorVersionNumber(2)
                                .patchVersionNumber(3)
                                .firstSequenceVersionNumber(4)
                                .type(SlsVersionType.RELEASE_SNAPSHOT)
                                .build()),
                Arguments.of(
                        0,
                        SlsVersion.valueOf("1.2.3"),
                        new OrderableSlsVersion.Builder()
                                .value("1.2.3")
                                .majorVersionNumber(1)
                                .minorVersionNumber(2)
                                .patchVersionNumber(3)
                                .type(SlsVersionType.RELEASE)
                                .build()),
                Arguments.of(
                        0,
                        SlsVersion.valueOf("1.2.3-rc4-5-gabcdef"),
                        new OrderableSlsVersion.Builder()
                                .value("1.2.3-rc4-5-gabcdef")
                                .majorVersionNumber(1)
                                .minorVersionNumber(2)
                                .patchVersionNumber(3)
                                .firstSequenceVersionNumber(4)
                                .secondSequenceVersionNumber(5)
                                .type(SlsVersionType.RELEASE_CANDIDATE_SNAPSHOT)
                                .build()),
                Arguments.of(
                        0,
                        SlsVersion.valueOf("1.2.3-rc4"),
                        new OrderableSlsVersion.Builder()
                                .value("1.2.3-rc1")
                                .majorVersionNumber(1)
                                .minorVersionNumber(2)
                                .patchVersionNumber(3)
                                .firstSequenceVersionNumber(4)
                                .type(SlsVersionType.RELEASE_CANDIDATE)
                                .build()),
                Arguments.of(
                        0,
                        SlsVersion.valueOf("1.2.3-abcdef"),
                        new NonOrderableSlsVersion.Builder()
                                .value("1.2.3-abcdef")
                                .majorVersionNumber(1)
                                .minorVersionNumber(2)
                                .patchVersionNumber(3)
                                .type(SlsVersionType.NON_ORDERABLE)
                                .build()));
    }

    private static void writeObject(Path path, Object object) throws Exception {
        Files.createDirectories(path.getParent());

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(path))) {
            objectOutputStream.writeObject(object);
        }
    }

    private static <T> T readObject(Path path, Class<T> objectClass) throws Exception {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(path))) {
            Object object = objectInputStream.readObject();
            assertThat(object).isInstanceOf(objectClass);
            return objectClass.cast(object);
        }
    }
}

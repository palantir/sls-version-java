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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
@SuppressWarnings({"checkstyle:hideutilityclassconstructor", "VisibilityModifier", "DesignForExtension"})
public class ComparatorBenchmark {

    private static final ImmutableList<String> orderableVersions = ImmutableList.of(
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

    public static final Random RANDOM = new Random(42);

    private List<OrderableSlsVersion> versions;

    @Param({"1", "100", "10000"})
    int size;

    @Setup
    public void beforeAll() {
        versions = IntStream.range(0, size)
                .mapToObj(i -> {
                    int value = RANDOM.nextInt(100);
                    if (value < orderableVersions.size()) {
                        return OrderableSlsVersion.valueOf(orderableVersions.get(value));
                    }
                    return OrderableSlsVersion.valueOf(value + "." + value + "." + i);
                })
                .sorted(VersionComparator.INSTANCE)
                .collect(Collectors.toList());
    }

    @Setup(Level.Iteration)
    public void beforeEach() {
        Collections.shuffle(versions, RANDOM);
    }

    @Benchmark
    public List<OrderableSlsVersion> versionComparator() {
        versions.sort(VersionComparator.INSTANCE);
        return versions;
    }

    public static void main(String[] _args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ComparatorBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .forks(1)
                .threads(1)
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(3))
                .build();
        new Runner(opt).run();
    }
}

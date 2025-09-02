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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
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
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(4)
@SuppressWarnings({
    "checkstyle:hideutilityclassconstructor",
    "VisibilityModifier",
    "DesignForExtension",
    "ImmutableEnumChecker"
})
public class SlsVersionComparisonBenchmark {

    public enum Version {
        RELEASE("0.16.0"),
        SNAPSHOT("0.16.0-8-g116b425"),
        RC("0.16.0-rc1"),
        RC_SNAPSHOT("0.16.0-rc1-8-g116b425"),
        ;

        private final OrderableSlsVersion version1;
        // Used to avoid comparing the object's identities when comparing the same version
        private final OrderableSlsVersion version2;

        Version(String string) {
            this.version1 = OrderableSlsVersion.valueOf(string);
            this.version2 = OrderableSlsVersion.valueOf(string);
        }
    }

    @Param
    Version version1;

    @Param
    Version version2;

    @Benchmark
    public int compare() {
        return version1.version1.compareTo(version2.version2);
    }

    public static void main(String[] _args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SlsVersionComparisonBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .forks(1)
                .threads(4)
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(3))
                .build();
        new Runner(opt).run();
    }
}

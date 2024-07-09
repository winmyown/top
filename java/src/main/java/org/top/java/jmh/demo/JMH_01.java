package org.top.java.jmh.demo;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class JMH_01 {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations =1)
    @Fork(0)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughput() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(10);
    }

    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(JMH_01.class.getSimpleName())
//                .forks(1)
//                .build();
        Options opt = new OptionsBuilder()
                .include(JMH_01.class.getSimpleName())
                .forks(1)
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(opt).run();
    }
}

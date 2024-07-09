/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.top.java.jmh.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class JMHSample_02_BenchmarkModes {

    /*
     * JMH在期间为您生成大量用于基准测试的合成代码
     * 基准编译。JMH可以批量测量基准方法
     * 模式。用户可以选择默认的基准测试模式，并带有特殊的
     * 注释，或通过运行时选项选择/覆盖模式。
     *
     * 在这种情况下，我们开始衡量一些有用的东西。请注意，我们的
     * 有效载荷代码可能会抛出异常，我们只需声明它们即可
     * 被抛出。如果代码抛出实际的异常，则基准测试
     * 执行将因错误而停止。
     *
     * 当您对某些特定行为感到困惑时，通常会有所帮助
     * 查看生成的代码。您可能会看到代码没有执行
     * 你打算它做的事情。好的实验总是会跟进
     * 实验设置，并交叉检查生成的代码是很重要的
     * 该后续行动的一部分。
     *
     * 此特定示例生成的代码位于
     * 目标/生成源/注释/.../JMHSample_02_BenchmarkModes.java
     */

    /*
     * Mode.Throughput 在其 Javadoc 中所述，通过以下方式测量原始吞吐量
     * 在有时限的迭代中连续调用基准方法，以及
     * 计算我们执行该方法的次数。
     *
     * 我们正在使用特殊注释来选择要测量的单位，
     * 尽管您可以使用默认值。
     */

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughput() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     模式AverageTime测量平均执行时间，它确实做到了
    *以类似于Mode的方式。吞吐量
    *
    *有人可能会说这是吞吐量的倒数，事实确实如此。
    *不过，有些工作负载的测量时间更方便。
     */

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureAvgTime() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     * Mode.SampleTime samples the execution time. With this mode, we are
     * still running the method in a time-bound iteration, but instead of
     * measuring the total time, we measure the time spent in *some* of
     * the benchmark method calls.
     *
     * This allows us to infer the distributions, percentiles, etc.
     *
     * JMH also tries to auto-adjust sampling frequency: if the method
     * is long enough, you will end up capturing all the samples.
     */
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureSamples() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     * Mode.SingleShotTime measures the single method invocation time. As the Javadoc
     * suggests, we do only the single benchmark method invocation. The iteration
     * time is meaningless in this mode: as soon as benchmark method stops, the
     * iteration is over.
     *
     * This mode is useful to do cold startup tests, when you specifically
     * do not want to call the benchmark method continuously.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureSingleShot() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     * We can also ask for multiple benchmark modes at once. All the tests
     * above can be replaced with just a single test like this:
     */
    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime, Mode.SingleShotTime})
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureMultiple() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     * Or even...
     */

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureAll() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * 您应该会看到同一基准测试的不同运行模式。
     * 注意单位不同，分数彼此一致。
     *
     * 您可以运行此测试：
     *
     * a） 通过命令行：
     * $ mvn 全新安装
     * $ java -jar 目标/benchmarks.jar JMHSample_02 -f 1
     * （我们请求了一个单分支;还有其他选项，参见 -h）
     *
     * b） 通过Java API：
     * （请参阅 JMH 主页，了解从 IDE 运行时可能存在的注意事项：
     * http://openjdk.java.net/projects/code-tools/jmh/）
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_02_BenchmarkModes.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}

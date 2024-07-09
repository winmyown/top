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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class JMHSample_01_HelloWorld {

    /**
     * 这是我们的第一个基准测试方法。
     *
     * JMH 的工作方式如下：用户用 @Benchmark 注释方法，以及
     * 然后 JMH 生成生成的代码以运行此特定基准测试作为
     * 尽可能可靠。一般来说，人们可能会考虑@Benchmark方法
     * 作为基准“有效载荷”，我们想要测量的东西。这
     * 周围的基础设施由线束本身提供。
     *
     * 阅读 Javadoc 以获取完整的语义和 @Benchmark 注解
     *限制。在这一点上，我们只注意到方法名称是
     * 非必要，仅要求方法标有
     * @Benchmark.您可以在同一个中拥有多个基准测试方法
     *类。
     *
     * 注意：如果基准测试方法永不完成，那么 JMH 运行将永不完成
     *也。如果从方法主体抛出异常，JMH 运行将结束
     * 突然对于此基准测试，JMH 将运行下一个基准测试。
     *列表。
     *
     * 尽管这个基准测试“什么都没有”，但它是一个很好的展示
     * 开销基础结构对您在方法中测量的代码的影响。
     * 没有神奇的基础设施不会产生任何开销，而且确实如此
     * 重要的是要知道您正在处理的基础设施开销是多少。你
     * 可能会发现这个想法在未来的示例中通过拥有
     * “基线”测量值进行比较。
     */

    @Benchmark
    public void wellHelloThere() {
        //此方法有意留空.
    }

    /**
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * 预计您会看到具有大量迭代的运行，并且
     * 非常大的吞吐量数字。您可以将其视为对
     * 利用每个方法调用的开销。在我们的大多数测量中，它是
     * 每次调用精确到几个周期。
     *
     * a） 通过命令行：
     * $ mvn 全新安装
     * $ java -jar 目标/benchmarks.jar JMHSample_01
     *
     * JMH 生成独立的 JAR，将 JMH 与其捆绑在一起。
     * JMH 的运行时选项与“-h”一起使用：
     * $ java -jar 目标/benchmarks.jar -h
     *
     * b） 通过Java API：
     * （请参阅 JMH 主页，了解从 IDE 运行时可能存在的注意事项：
     * http://openjdk.java.net/projects/code-tools/jmh/）
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHSample_01_HelloWorld.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}

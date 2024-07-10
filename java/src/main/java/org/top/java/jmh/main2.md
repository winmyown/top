在使用 JMH 进行基准测试时，测试的整个流程包括准备阶段、预热阶段、测量阶段和收尾阶段。每个阶段都有特定的目的和操作，JMH 提供了多个注解来配置和控制这些阶段的行为。以下是整个流程的详细说明：

### JMH 基准测试流程

1. **准备阶段（Setup）**：
    - 在基准测试开始前进行准备工作，例如初始化测试数据和设置环境。
    - 主要由 `@Setup` 注解控制。`@Setup` 可以指定在不同的级别（试验级别、迭代级别）运行准备工作。

2. **预热阶段（Warmup）**：
    - 在正式测量前运行一段时间，以便让 JVM 进行优化，如 JIT 编译。这有助于获得更稳定的测试结果。
    - 由 `@Warmup` 注解控制，可以配置预热的迭代次数、每次迭代的时间等。

3. **测量阶段（Measurement）**：
    - 正式的基准测试阶段，JMH 会收集数据并生成结果。
    - 由 `@Measurement` 注解控制，可以配置测量的迭代次数、每次迭代的时间等。

4. **收尾阶段（TearDown）**：
    - 在基准测试结束后进行清理工作，如释放资源。
    - 主要由 `@TearDown` 注解控制。`@TearDown` 可以指定在不同的级别（试验级别、迭代级别）运行清理工作。

### 注解作用详解

#### 基本注解

- **@Benchmark**:
    - 标记一个基准测试方法。JMH 会在测试过程中反复调用这个方法。
  ```java
  @Benchmark
  public void testMethod() {
      // 基准测试代码
  }
  ```

#### 配置注解

- **@State**:
    - 定义测试状态，用于共享基准测试中的数据。
    - `Scope.Thread` 表示每个线程有一个实例；`Scope.Benchmark` 表示所有线程共享一个实例。
  ```java
  @State(Scope.Thread)
  public static class MyState {
      // 状态变量
  }
  ```

- **@BenchmarkMode**:
    - 配置基准测试模式，如吞吐量、平均时间、采样时间等。
  ```java
  @BenchmarkMode(Mode.Throughput)
  @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
  public void testMethod() {
      // 基准测试代码
  }
  ```

- **@OutputTimeUnit**:
    - 设置基准测试结果的时间单位。
  ```java
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void testMethod() {
      // 基准测试代码
  }
  ```

- **@Warmup**:
    - 配置预热设置，包括预热迭代次数和每次迭代的时间。
  ```java
  @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
  public void testMethod() {
      // 基准测试代码
  }
  ```

- **@Measurement**:
    - 配置测量设置，包括测量迭代次数和每次迭代的时间。
  ```java
  @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
  public void testMethod() {
      // 基准测试代码
  }
  ```

- **@Fork**:
    - 配置 fork 设置，指定基准测试运行的次数，每次运行在一个新的 JVM 实例中。
  ```java
  @Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
  public void testMethod() {
      // 基准测试代码
  }
  ```

- **@Threads**:
    - 配置测试运行的线程数。
  ```java
  @Threads(4)
  public void testMethod() {
      // 基准测试代码
  }
  ```

#### 其他注解

- **@Setup**:
    - 标记准备方法，在基准测试前执行。可以指定级别（如试验级别、迭代级别）。
  ```java
  @Setup(Level.Trial)
  public void setUp() {
      // 准备工作
  }
  ```

- **@TearDown**:
    - 标记清理方法，在基准测试后执行。可以指定级别（如试验级别、迭代级别）。
  ```java
  @TearDown(Level.Trial)
  public void tearDown() {
      // 清理工作
  }
  ```

### 示例流程

以下是一个包含所有阶段的完整 JMH 基准测试示例：

```java
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class MyBenchmark {

    private int a;
    private int b;

    @Setup(Level.Trial)
    public void setUp() {
        a = 1;
        b = 2;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(1)
    public int add() {
        return a + b;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        // 清理工作
    }
}
```

### 运行基准测试

创建一个主类来运行基准测试：

```java
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(MyBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
```

通过上述步骤和代码示例，你可以全面了解 JMH 运行基准测试的整个流程，以及如何使用注解来配置和控制每个阶段的行为。
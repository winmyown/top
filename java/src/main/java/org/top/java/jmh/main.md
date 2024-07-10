# JMH

[详细示例介绍](https://heapdump.cn/article/2985869)

| 注解            | 参数                               | 说明                                                                                     |
|-----------------|----------------------------------|-----------------------------------------------------------------------------------------|
| @Benchmark      | 无                                | 标记方法为基准测试方法。                                                                |
| @State          | `Scope`                          | 定义基准测试的状态。范围包括：`Scope.Benchmark`，`Scope.Group`，`Scope.Thread`。          |
| @Setup          | `Level`                          | 在基准测试之前执行的方法。级别包括：`Level.Trial`，`Level.Iteration`，`Level.Invocation`。|
| @TearDown       | `Level`                          | 在基准测试之后执行的方法。级别包括：`Level.Trial`，`Level.Iteration`，`Level.Invocation`。|
| @Param          | 无                                | 用于参数化测试。直接在字段上使用，并提供一组可能的值。                                    |
| @OutputTimeUnit | `TimeUnit`                       | 指定输出的时间单位。包括：`TimeUnit.SECONDS`，`TimeUnit.MILLISECONDS`，`TimeUnit.MICROSECONDS`，`TimeUnit.NANOSECONDS`。|
| @BenchmarkMode  | `Mode`                           | 指定基准测试模式。模式包括：`Mode.Throughput`，`Mode.AverageTime`，`Mode.SampleTime`，`Mode.SingleShotTime`，`Mode.All`。|
| @Warmup         | `iterations` `time` `timeUnit` `batchSize` | 配置预热阶段。参数包括：预热迭代次数，每次预热迭代的时间，时间单位（默认为 `TimeUnit.SECONDS`），每次预热迭代的批量大小（默认为 1）。|
| @Measurement    | `iterations` `time` `timeUnit` `batchSize` | 配置测量阶段。参数包括：测量迭代次数，每次测量迭代的时间，时间单位（默认为 `TimeUnit.SECONDS`），每次测量迭代的批量大小（默认为 1）。|
| @Fork           | `value` `jvmArgs` `jvmArgsAppend` `jvm` `warmup` | 配置测试的 fork 次数。参数包括：fork 次数，JVM 参数，附加的 JVM 参数，指定 JVM 版本，是否在 fork 时进行预热。|
| @Threads        | 无                                | 配置用于测试的线程数。直接在方法上使用。                                                  |
| @Group          | `value`                          | 将方法分配到组中，以便进行组内测试。参数为组名。                                          |
| @GroupThreads   | `value`                          | 指定组内的线程数。参数为线程数。                                                         |
| @CompilerControl| `CompilerControl.Mode`           | 控制编译器的行为。模式包括：`CompilerControl.Mode.DONT_INLINE`，`CompilerControl.Mode.INLINE`，`CompilerControl.Mode.EXCLUDE`。|


## 注解
<details>
<summary>Benchmark 标记方法为基准测试方法</summary>
tse

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Benchmark {

}

```
</details>



<details>
<summary></summary>

```java
asdf
```
</details>

<details>
<summary>sdfa</summary>

```java
/**
 * <p>线程注释提供要运行的默认线程数。</p>
 *
 * <p>此注释可以放在 {@link Benchmark} 方法以生效
 * 仅在该方法上，或在封闭类实例上具有效果
 * 在类中的所有 {@link Benchmark} 方法上。此注释可能是
 * 被运行时选项覆盖。</p>
 */
@Inherited
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Threads {

    /**
     * The magic value for MAX threads.
     * This means Runtime.getRuntime().availableProcessors() threads.
     */
    int MAX = -1;

    /**
     * @return Number of threads; use Threads.MAX to run with all available threads.
     */
    int value();

}
```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary></summary>

```java

```
</details>

<details>
<summary>Warmup</summary>
预热注解允许设置基准测试的默认预热参数。
可以将此注释放在 Benchmark method 上，以便仅对该方法起作用，或者放在封闭的类实例上，以便对 Benchmark 类中的所有方法产生影响。此注释可能会被运行时选项覆盖。
另请参见：
Measurement

```java
import org.openjdk.jmh.annotations.Warmup;

@Warmup
public void test() {

}
```

</details>
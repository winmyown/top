package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:10
 */

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 一个 {@link ExecutorService}，可以调度命令在给定的延迟后运行，或定期执行。
 *
 * <p>{@code schedule} 方法创建具有各种延迟的任务，并返回一个任务对象，该对象可用于取消或检查执行情况。
 * {@code scheduleAtFixedRate} 和 {@code scheduleWithFixedDelay} 方法创建并执行定期运行的任务，直到任务被取消。
 *
 * <p>使用 {@link Executor#execute(Runnable)} 和 {@link ExecutorService} 的 {@code submit} 方法提交的命令的请求延迟为零。
 * 在 {@code schedule} 方法中，也允许零和负延迟（但不允许负的周期），并将其视为立即执行的请求。
 *
 * <p>所有 {@code schedule} 方法都接受相对的延迟和周期作为参数，而不是绝对时间或日期。
 * 将 {@link java.util.Date} 表示的绝对时间转换为所需的格式很简单。
 * 例如，要在某个未来的 {@code date} 进行调度，可以使用：
 * {@code schedule(task, date.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)}。
 * 但是请注意，由于网络时间同步协议、时钟漂移或其他因素，相对延迟的到期不一定与任务被启用时的当前 {@code Date} 重合。
 *
 * <p>{@link Executors} 类为此包中提供的 ScheduledExecutorService 实现提供了便捷的工厂方法。
 *
 * <h3>使用示例</h3>
 *
 * 下面是一个类，其中包含一个方法，设置了一个 ScheduledExecutorService 以每 10 秒响一次蜂鸣器，持续一小时：
 *
 *  <pre> {@code
 * import static java.util.concurrent.TimeUnit.*;
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler =
 *     Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *     final Runnable beeper = new Runnable() {
 *       public void run() { System.out.println("beep"); }
 *     };
 *     final ScheduledFuture<?> beeperHandle =
 *       scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *     scheduler.schedule(new Runnable() {
 *       public void run() { beeperHandle.cancel(true); }
 *     }, 60 * 60, SECONDS);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * 作者 Doug Lea
 */
public interface ScheduledExecutorService extends ExecutorService {

    /**
     * 创建并执行一个一次性动作，该动作在给定的延迟后启用。
     *
     * @param command 要执行的任务
     * @param delay 从现在开始延迟执行的时间
     * @param unit 延迟参数的时间单位
     * @return 一个代表任务待完成的 ScheduledFuture，其 {@code get()} 方法在完成时将返回 {@code null}
     * @throws RejectedExecutionException 如果任务无法调度执行
     * @throws NullPointerException 如果 command 为 null
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);

    /**
     * 创建并执行一个在给定延迟后启用的 ScheduledFuture。
     *
     * @param callable 要执行的函数
     * @param delay 从现在开始延迟执行的时间
     * @param unit 延迟参数的时间单位
     * @param <V> callable 结果的类型
     * @return 一个 ScheduledFuture，可用于提取结果或取消任务
     * @throws RejectedExecutionException 如果任务无法调度执行
     * @throws NullPointerException 如果 callable 为 null
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);

    /**
     * 创建并执行一个定期动作，该动作首先在给定的初始延迟后启用，然后以给定的周期运行；
     * 即执行将在 {@code initialDelay} 之后开始，然后是 {@code initialDelay+period}，然后是
     * {@code initialDelay + 2 * period}，依此类推。
     * 如果任务的任何执行遇到异常，则后续执行将被抑制。
     * 否则，任务仅通过取消或执行器的终止而终止。
     * 如果此任务的任何执行时间超过其周期，则后续执行可能会延迟开始，但不会并发执行。
     *
     * @param command 要执行的任务
     * @param initialDelay 第一次执行的延迟时间
     * @param period 连续执行之间的时间间隔
     * @param unit initialDelay 和 period 参数的时间单位
     * @return 一个代表任务待完成的 ScheduledFuture，其 {@code get()} 方法在取消时将抛出异常
     * @throws RejectedExecutionException 如果任务无法调度执行
     * @throws NullPointerException 如果 command 为 null
     * @throws IllegalArgumentException 如果 period 小于或等于零
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    /**
     * 创建并执行一个定期动作，该动作首先在给定的初始延迟后启用，然后在一个执行终止和下一次执行开始之间
     * 以给定的延迟时间运行。 如果任务的任何执行遇到异常，则后续执行将被抑制。
     * 否则，任务仅通过取消或执行器的终止而终止。
     *
     * @param command 要执行的任务
     * @param initialDelay 第一次执行的延迟时间
     * @param delay 一次执行终止和下一次执行开始之间的延迟
     * @param unit initialDelay 和 delay 参数的时间单位
     * @return 一个代表任务待完成的 ScheduledFuture，其 {@code get()} 方法在取消时将抛出异常
     * @throws RejectedExecutionException 如果任务无法调度执行
     * @throws NullPointerException 如果 command 为 null
     * @throws IllegalArgumentException 如果 delay 小于或等于零
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

}


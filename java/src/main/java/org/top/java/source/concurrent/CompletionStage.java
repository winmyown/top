package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午5:39
 */

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 一个可能是异步计算的阶段，它在另一个CompletionStage完成时执行某个动作或计算一个值。
 * 一个阶段在其计算终止时完成，但这可能会触发其他依赖阶段。该接口定义的功能只有几种基本形式，
 * 这些形式扩展为一组更大的方法，以涵盖各种使用风格：<ul>
 *
 * <li>阶段执行的计算可以表示为Function、Consumer或Runnable（使用包含<em>apply</em>、<em>accept</em>或
 * <em>run</em>的名称的方法），具体取决于它是否需要参数和/或产生结果。
 * 例如，{@code stage.thenApply(x -> square(x)).thenAccept(x -> System.out.print(x)).thenRun(() -> System.out.println())}。
 * 另一种形式（<em>compose</em>）应用的是阶段本身的函数，而不是其结果。</li>
 *
 * <li>一个阶段的执行可以通过一个单阶段的完成来触发，或通过两个阶段的完成来触发，或者是两者之一的完成来触发。
 * 单阶段依赖通过<em>then</em>前缀的方法安排。通过完成两个阶段的<em>both</em>可以<em>combine</em>它们的结果或效果，
 * 使用相应命名的方法。由两个阶段的<em>either</em>完成触发时，对于哪个结果或效果用于依赖阶段的计算不作保证。</li>
 *
 * <li>阶段之间的依赖关系控制计算的触发，但不保证任何特定的顺序。此外，可以通过三种方式安排新阶段的计算执行：
 * 默认执行、默认异步执行（使用带有<em>async</em>后缀并利用阶段的默认异步执行功能的方法），或者自定义（通过提供的{@link Executor}）。
 * 默认和异步模式的执行特性由CompletionStage实现定义，而非此接口定义。具有显式Executor参数的方法可能具有任意执行特性，
 * 并且可能不支持并发执行，但它们以适应异步的方式进行处理。
 *
 * <li>两种方法形式支持无论触发阶段是正常完成还是异常完成的处理：{@link #whenComplete whenComplete}方法允许无论结果如何，
 * 注入一个操作，并在完成时保留结果。{@link #handle handle}方法还允许阶段计算一个替换结果，可能会启用其他依赖阶段的进一步处理。
 * 在所有其他情况下，如果阶段的计算突然以（未经检查的）异常或错误终止，则所有依赖于其完成的阶段也将异常完成，
 * 并抛出一个{@link CompletionException}，该异常持有原始异常作为其原因。
 * 如果一个阶段依赖于两个阶段的<em>both</em>，并且两个阶段都异常完成，则CompletionException可能对应于其中一个异常。
 * 如果一个阶段依赖于两者之一，并且其中一个阶段异常完成，则不能保证依赖阶段是正常完成还是异常完成。
 * 对于{@code whenComplete}方法，如果提供的操作本身遇到异常，则该阶段会使用此异常异常完成（如果尚未完成异常的话）。</li>
 * </ul>
 *
 * <p>所有方法都遵循上述触发、执行和异常完成规范（这些规范不会在各个方法的说明中重复）。
 * 此外，虽然用于传递完成结果的参数（即，类型为{@code T}的参数）可以为null，但为其他参数传递null值将导致抛出
 * {@link NullPointerException}。</p>
 *
 * <p>此接口不定义方法用于初始创建、强制正常或异常完成、探测完成状态或结果，或等待阶段的完成。
 * CompletionStage的实现可能会根据需要提供实现这些效果的方式。方法{@link #toCompletableFuture}
 * 提供了在此接口的不同实现之间进行互操作的共同转换类型。</p>
 *
 * @作者 Doug Lea
 * @自 1.8
 */
public interface CompletionStage<T> {

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的结果作为参数执行提供的函数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的默认异步执行功能，使用此阶段的结果作为参数执行提供的函数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用提供的Executor执行提供的函数，使用此阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param fn 计算返回CompletionStage值的函数
     * @param executor 用于异步执行的executor
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的结果作为参数执行提供的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenAccept(Consumer<? super T> action);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的默认异步执行功能，使用此阶段的结果作为参数执行提供的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用提供的Executor执行提供的操作，使用此阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @param executor 用于异步执行的executor
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenRun(Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用此阶段的默认异步执行功能执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenRunAsync(Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段正常完成时，使用提供的Executor执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param action 在完成返回的CompletionStage之前执行的操作
     * @param executor 用于异步执行的executor
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用两个结果作为参数执行提供的函数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 另一个CompletionStage的结果类型
     * @param <V> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                                 BiFunction<? super T, ? super U, ? extends V> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段正常完成时，使用此阶段的默认异步执行功能执行提供的函数，使用两个结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 另一个CompletionStage的结果类型
     * @param <V> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                      BiFunction<? super T, ? super U, ? extends V> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段正常完成时，使用提供的Executor执行提供的函数，使用两个结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param executor 用于异步执行的executor
     * @param <U> 另一个CompletionStage的结果类型
     * @param <V> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                      BiFunction<? super T, ? super U, ? extends V> fn,
                                                      Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用两个结果作为参数执行提供的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param <U> 另一个CompletionStage的结果类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                    BiConsumer<? super T, ? super U> action);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用此阶段的默认异步执行功能执行提供的操作，使用两个结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param <U> 另一个CompletionStage的结果类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                         BiConsumer<? super T, ? super U> action);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用提供的Executor执行提供的操作，使用两个结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param executor 用于异步执行的executor
     * @param <U> 另一个CompletionStage的结果类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                         BiConsumer<? super T, ? super U> action,
                                                         Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用此阶段的默认异步执行功能执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段和其他给定阶段都正常完成时，使用提供的Executor执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param executor 用于异步执行的executor
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用完成的阶段的结果作为参数执行提供的函数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用此阶段的默认异步执行功能执行提供的函数，使用完成的阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用提供的Executor执行提供的函数，使用完成的阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param fn 计算返回CompletionStage值的函数
     * @param executor 用于异步执行的executor
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
                                                     Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用完成的阶段的结果作为参数执行提供的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用此阶段的默认异步执行功能执行提供的操作，使用完成的阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用提供的Executor执行提供的操作，使用完成的阶段的结果作为参数。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param executor 用于异步执行的executor
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
                                                   Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用此阶段的默认异步执行功能执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);

    /**
     * 返回一个新的CompletionStage，当此阶段或另一个给定阶段正常完成时，使用提供的Executor执行给定的操作。
     *
     * 有关异常完成的规则，请参阅{@link CompletionStage}文档。
     *
     * @param other 另一个CompletionStage
     * @param action 执行的操作
     * @param executor 用于异步执行的executor
     * @return 新的CompletionStage
     */
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段完成时（无论是正常完成还是异常完成），
     * 使用此阶段的结果和异常作为参数执行提供的函数。
     *
     * 当此阶段完成时，给定的函数将使用结果（如果没有则为null）和异常（如果没有则为null）
     * 作为参数执行，并且函数的结果将用于完成返回的阶段。
     *
     * @param fn 用来计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段完成时（无论是正常完成还是异常完成），
     * 使用此阶段的默认异步执行功能，使用此阶段的结果和异常作为参数执行提供的函数。
     *
     * 当此阶段完成时，给定的函数将使用结果（如果没有则为null）和异常（如果没有则为null）
     * 作为参数执行，并且函数的结果将用于完成返回的阶段。
     *
     * @param fn 用来计算返回CompletionStage值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    /**
     * 返回一个新的CompletionStage，当此阶段完成时（无论是正常完成还是异常完成），
     * 使用提供的Executor，使用此阶段的结果和异常作为参数执行提供的函数。
     *
     * 当此阶段完成时，给定的函数将使用结果（如果没有则为null）和异常（如果没有则为null）
     * 作为参数执行，并且函数的结果将用于完成返回的阶段。
     *
     * @param fn 用来计算返回CompletionStage值的函数
     * @param executor 用于异步执行的executor
     * @param <U> 函数的返回类型
     * @return 新的CompletionStage
     */
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                              Executor executor);

    /**
     * 返回一个新的CompletionStage，当此阶段异常完成时，使用此阶段的异常作为参数执行提供的函数。
     * 否则，如果此阶段正常完成，则返回的阶段也正常完成并带有相同的值。
     *
     * @param fn 用来计算返回CompletionStage值的函数
     * @return 新的CompletionStage
     */
    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);

    /**
     * 返回一个{@link CompletableFuture}，其维持与此阶段相同的完成属性。
     * 如果此阶段已经是一个CompletableFuture，则此方法可能返回此阶段本身。
     * 否则，此方法的调用效果可能等效于{@code thenApply(x -> x)}，但返回的实例类型为{@code CompletableFuture}。
     *
     * @return CompletableFuture
     * @throws UnsupportedOperationException 如果此实现不支持与CompletableFuture互操作
     */
    public CompletableFuture<T> toCompletableFuture();
}




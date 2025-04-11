package org.top.java.netty.source.buffer.search;

import org.top.java.netty.source.buffer.ByteBuf;
import org.top.java.netty.source.buffer.ByteBufUtil;

/**
 * Base class for precomputed factories that create {@link SearchProcessor}s.
 * <br>
 * Different factories implement different search algorithms with performance characteristics that
 * depend on a use case, so it is advisable to benchmark a concrete use case with different algorithms
 * before choosing one of them.
 * <br>
 * A concrete instance of {@link AbstractSearchProcessorFactory} is built for searching for a concrete sequence of bytes
 * (the {@code needle}), it contains precomputed data needed to perform the search, and is meant to be reused
 * whenever searching for the same {@code needle}.
 * <br>
 * <b>Note:</b> implementations of {@link SearchProcessor} scan the {@link ByteBuf} sequentially,
 * one byte after another, without doing any random access. As a result, when using {@link SearchProcessor}
 * with such methods as {@link ByteBuf#forEachByte}, these methods return the index of the last byte
 * of the found byte sequence within the {@link ByteBuf} (which might feel counterintuitive,
 * and different from {@link ByteBufUtil#indexOf} which returns the index of the first byte
 * of found sequence).
 * <br>
 * A {@link SearchProcessor} is implemented as a
 * <a href="https://en.wikipedia.org/wiki/Finite-state_machine">Finite State Automaton</a> that contains a
 * small internal state which is updated with every byte processed. As a result, an instance of {@link SearchProcessor}
 * should not be reused across independent search sessions (eg. for searching in different
 * {@link ByteBuf}s). A new instance should be created with {@link AbstractSearchProcessorFactory} for
 * every search session. However, a {@link SearchProcessor} can (and should) be reused within the search session,
 * eg. when searching for all occurrences of the {@code needle} within the same {@code haystack}. That way, it can
 * also detect overlapping occurrences of the {@code needle} (eg. a string "ABABAB" contains two occurrences of "BAB"
 * that overlap by one character "B"). For this to work correctly, after an occurrence of the {@code needle} is
 * found ending at index {@code idx}, the search should continue starting from the index {@code idx + 1}.
 * <br>
 * Example (given that the {@code haystack} is a {@link ByteBuf} containing "ABABAB" and
 * the {@code needle} is "BAB"):
 * <pre>
 *     SearchProcessorFactory factory =
 *         SearchProcessorFactory.newKmpSearchProcessorFactory(needle.getBytes(CharsetUtil.UTF_8));
 *     SearchProcessor processor = factory.newSearchProcessor();
 *
 *     int idx1 = haystack.forEachByte(processor);
 *     // idx1 is 3 (index of the last character of the first occurrence of the needle in the haystack)
 *
 *     int continueFrom1 = idx1 + 1;
 *     // continue the search starting from the next character
 *
 *     int idx2 = haystack.forEachByte(continueFrom1, haystack.readableBytes() - continueFrom1, processor);
 *     // idx2 is 5 (index of the last character of the second occurrence of the needle in the haystack)
 *
 *     int continueFrom2 = idx2 + 1;
 *     // continue the search starting from the next character
 *
 *     int idx3 = haystack.forEachByte(continueFrom2, haystack.readableBytes() - continueFrom2, processor);
 *     // idx3 is -1 (no more occurrences of the needle)
 *
 *     // After this search session is complete, processor should be discarded.
 *     // To search for the same needle again, reuse the same factory to get a new SearchProcessor.
 * </pre>
 */

/**
 * 用于创建 {@link SearchProcessor} 的预计算工厂的基类。
 * <br>
 * 不同的工厂实现了不同的搜索算法，其性能特性取决于具体的使用场景，因此在选择算法之前，建议对具体的使用场景进行基准测试。
 * <br>
 * {@link AbstractSearchProcessorFactory} 的具体实例是为搜索特定的字节序列（即 {@code needle}）而构建的，它包含了执行搜索所需的预计算数据，并且可以重复使用以搜索相同的 {@code needle}。
 * <br>
 * <b>注意：</b> {@link SearchProcessor} 的实现会顺序扫描 {@link ByteBuf}，一个字节接一个字节，而不进行任何随机访问。因此，当使用 {@link SearchProcessor} 与诸如 {@link ByteBuf#forEachByte} 之类的方法时，这些方法返回的是 {@link ByteBuf} 中找到的字节序列的最后一个字节的索引（这可能会让人觉得反直觉，与 {@link ByteBufUtil#indexOf} 不同，后者返回的是找到的序列的第一个字节的索引）。
 * <br>
 * {@link SearchProcessor} 被实现为一个
 * <a href="https://en.wikipedia.org/wiki/Finite-state_machine">有限状态自动机</a>，它包含一个小的内部状态，该状态会随着每个处理的字节而更新。因此，不应在独立的搜索会话之间重用 {@link SearchProcessor} 的实例（例如，在不同的 {@link ByteBuf} 中搜索）。对于每次搜索会话，应使用 {@link AbstractSearchProcessorFactory} 创建一个新实例。然而，在搜索会话内，应重用 {@link SearchProcessor}（例如，在同一个 {@code haystack} 中搜索所有 {@code needle} 的出现）。这样，它还可以检测到 {@code needle} 的重叠出现（例如，字符串 "ABABAB" 包含两个 "BAB" 的出现，它们重叠了一个字符 "B"）。为了正确实现这一点，在找到 {@code needle} 的出现并结束于索引 {@code idx} 后，搜索应从索引 {@code idx + 1} 继续。
 * <br>
 * 示例（假设 {@code haystack} 是一个包含 "ABABAB" 的 {@link ByteBuf}，且 {@code needle} 是 "BAB"）：
 * <pre>
 *     SearchProcessorFactory factory =
 *         SearchProcessorFactory.newKmpSearchProcessorFactory(needle.getBytes(CharsetUtil.UTF_8));
 *     SearchProcessor processor = factory.newSearchProcessor();
 *
 *     int idx1 = haystack.forEachByte(processor);
 *     // idx1 是 3（第一个 needle 出现的最后一个字符的索引）
 *
 *     int continueFrom1 = idx1 + 1;
 *     // 从下一个字符继续搜索
 *
 *     int idx2 = haystack.forEachByte(continueFrom1, haystack.readableBytes() - continueFrom1, processor);
 *     // idx2 是 5（第二个 needle 出现的最后一个字符的索引）
 *
 *     int continueFrom2 = idx2 + 1;
 *     // 从下一个字符继续搜索
 *
 *     int idx3 = haystack.forEachByte(continueFrom2, haystack.readableBytes() - continueFrom2, processor);
 *     // idx3 是 -1（没有更多的 needle 出现）
 *
 *     // 搜索会话完成后，应丢弃 processor。
 *     // 要再次搜索相同的 needle，请重用相同的工厂以获取新的 SearchProcessor。
 * </pre>
 */
public abstract class AbstractSearchProcessorFactory implements SearchProcessorFactory {

    /**
     * Creates a {@link SearchProcessorFactory} based on
     * <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">Knuth-Morris-Pratt</a>
     * string search algorithm. It is a reasonable default choice among the provided algorithms.
     * <br>
     * Precomputation (this method) time is linear in the size of input ({@code O(|needle|)}).
     * <br>
     * The factory allocates and retains an int array of size {@code needle.length + 1}, and retains a reference
     * to the {@code needle} itself.
     * <br>
     * Search (the actual application of {@link SearchProcessor}) time is linear in the size of
     * {@link ByteBuf} on which the search is performed ({@code O(|haystack|)}).
     * Every byte of {@link ByteBuf} is processed only once, sequentially.
     *
     * @param needle an array of bytes to search for
     * @return a new instance of {@link KmpSearchProcessorFactory} precomputed for the given {@code needle}
     */

    /**
     * 基于
     * <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">Knuth-Morris-Pratt</a>
     * 字符串搜索算法创建一个 {@link SearchProcessorFactory}。它是提供的算法中的一个合理默认选择。
     * <br>
     * 预计算（此方法）的时间与输入的大小成线性关系（{@code O(|needle|)}）。
     * <br>
     * 工厂分配并保留一个大小为 {@code needle.length + 1} 的 int 数组，并保留对 {@code needle} 本身的引用。
     * <br>
     * 搜索（实际应用 {@link SearchProcessor}）的时间与执行搜索的 {@link ByteBuf} 的大小成线性关系（{@code O(|haystack|)}）。
     * {@link ByteBuf} 的每个字节仅被顺序处理一次。
     *
     * @param needle 要搜索的字节数组
     * @return 一个为给定 {@code needle} 预计算的 {@link KmpSearchProcessorFactory} 新实例
     */
    public static KmpSearchProcessorFactory newKmpSearchProcessorFactory(byte[] needle) {
        return new KmpSearchProcessorFactory(needle);
    }

    /**
     * Creates a {@link SearchProcessorFactory} based on Bitap string search algorithm.
     * It is a jump free algorithm that has very stable performance (the contents of the inputs have a minimal
     * effect on it). The limitation is that the {@code needle} can be no more than 64 bytes long.
     * <br>
     * Precomputation (this method) time is linear in the size of the input ({@code O(|needle|)}).
     * <br>
     * The factory allocates and retains a long[256] array.
     * <br>
     * Search (the actual application of {@link SearchProcessor}) time is linear in the size of
     * {@link ByteBuf} on which the search is performed ({@code O(|haystack|)}).
     * Every byte of {@link ByteBuf} is processed only once, sequentially.
     *
     * @param needle an array <b>of no more than 64 bytes</b> to search for
     * @return a new instance of {@link BitapSearchProcessorFactory} precomputed for the given {@code needle}
     */

    /**
     * 基于Bitap字符串搜索算法创建一个{@link SearchProcessorFactory}。
     * 这是一个无跳跃的算法，具有非常稳定的性能（输入内容对其影响极小）。限制是{@code needle}的长度不能超过64字节。
     * <br>
     * 预计算（此方法）的时间与输入的大小成线性关系（{@code O(|needle|)}）。
     * <br>
     * 工厂分配并保留一个long[256]数组。
     * <br>
     * 搜索（实际应用{@link SearchProcessor}）的时间与执行搜索的{@link ByteBuf}的大小成线性关系（{@code O(|haystack|)}）。
     * {@link ByteBuf}的每个字节仅被顺序处理一次。
     *
     * @param needle 一个<b>不超过64字节</b>的数组，用于搜索
     * @return 一个新的{@link BitapSearchProcessorFactory}实例，已为给定的{@code needle}进行预计算
     */
    public static BitapSearchProcessorFactory newBitapSearchProcessorFactory(byte[] needle) {
        return new BitapSearchProcessorFactory(needle);
    }

}

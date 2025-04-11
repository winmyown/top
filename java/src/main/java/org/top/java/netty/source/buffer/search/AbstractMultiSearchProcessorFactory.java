package org.top.java.netty.source.buffer.search;

import org.top.java.netty.source.buffer.ByteBuf;

/**
 * Base class for precomputed factories that create {@link MultiSearchProcessor}s.
 * <br>
 * The purpose of {@link MultiSearchProcessor} is to perform efficient simultaneous search for multiple {@code needles}
 * in the {@code haystack}, while scanning every byte of the input sequentially, only once. While it can also be used
 * to search for just a single {@code needle}, using a {@link SearchProcessorFactory} would be more efficient for
 * doing that.
 * <br>
 * See the documentation of {@link AbstractSearchProcessorFactory} for a comprehensive description of common usage.
 * In addition to the functionality provided by {@link SearchProcessor}, {@link MultiSearchProcessor} adds
 * a method to get the index of the {@code needle} found at the current position of the {@link MultiSearchProcessor} -
 * {@link MultiSearchProcessor#getFoundNeedleId()}.
 * <br>
 * <b>Note:</b> in some cases one {@code needle} can be a suffix of another {@code needle}, eg. {@code {"BC", "ABC"}},
 * and there can potentially be multiple {@code needles} found ending at the same position of the {@code haystack}.
 * In such case {@link MultiSearchProcessor#getFoundNeedleId()} returns the index of the longest matching {@code needle}
 * in the array of {@code needles}.
 * <br>
 * Usage example (given that the {@code haystack} is a {@link ByteBuf} containing "ABCD" and the
 * {@code needles} are "AB", "BC" and "CD"):
 * <pre>
 *      MultiSearchProcessorFactory factory = MultiSearchProcessorFactory.newAhoCorasicSearchProcessorFactory(
 *          "AB".getBytes(CharsetUtil.UTF_8), "BC".getBytes(CharsetUtil.UTF_8), "CD".getBytes(CharsetUtil.UTF_8));
 *      MultiSearchProcessor processor = factory.newSearchProcessor();
 *
 *      int idx1 = haystack.forEachByte(processor);
 *      // idx1 is 1 (index of the last character of the occurrence of "AB" in the haystack)
 *      // processor.getFoundNeedleId() is 0 (index of "AB" in needles[])
 *
 *      int continueFrom1 = idx1 + 1;
 *      // continue the search starting from the next character
 *
 *      int idx2 = haystack.forEachByte(continueFrom1, haystack.readableBytes() - continueFrom1, processor);
 *      // idx2 is 2 (index of the last character of the occurrence of "BC" in the haystack)
 *      // processor.getFoundNeedleId() is 1 (index of "BC" in needles[])
 *
 *      int continueFrom2 = idx2 + 1;
 *
 *      int idx3 = haystack.forEachByte(continueFrom2, haystack.readableBytes() - continueFrom2, processor);
 *      // idx3 is 3 (index of the last character of the occurrence of "CD" in the haystack)
 *      // processor.getFoundNeedleId() is 2 (index of "CD" in needles[])
 *
 *      int continueFrom3 = idx3 + 1;
 *
 *      int idx4 = haystack.forEachByte(continueFrom3, haystack.readableBytes() - continueFrom3, processor);
 *      // idx4 is -1 (no more occurrences of any of the needles)
 *
 *      // This search session is complete, processor should be discarded.
 *      // To search for the same needles again, reuse the same {@link AbstractMultiSearchProcessorFactory}
 *      // to get a new MultiSearchProcessor.
 * </pre>
 */

/**
 * 用于创建 {@link MultiSearchProcessor} 的预计算工厂的基类。
 * <br>
 * {@link MultiSearchProcessor} 的目的是在顺序扫描输入的每一字节仅一次的情况下，高效地同时搜索多个 {@code needles}。
 * 虽然它也可以用于搜索单个 {@code needle}，但在这种情况下使用 {@link SearchProcessorFactory} 会更加高效。
 * <br>
 * 有关常见用法的全面描述，请参阅 {@link AbstractSearchProcessorFactory} 的文档。
 * 除了 {@link SearchProcessor} 提供的功能外，{@link MultiSearchProcessor} 还添加了一个方法，用于获取在当前 {@link MultiSearchProcessor} 位置找到的 {@code needle} 的索引 -
 * {@link MultiSearchProcessor#getFoundNeedleId()}。
 * <br>
 * <b>注意：</b>在某些情况下，一个 {@code needle} 可能是另一个 {@code needle} 的后缀，例如 {@code {"BC", "ABC"}}，
 * 并且可能有多个 {@code needles} 在 {@code haystack} 的同一位置结束匹配。
 * 在这种情况下，{@link MultiSearchProcessor#getFoundNeedleId()} 返回 {@code needles} 数组中最长匹配 {@code needle} 的索引。
 * <br>
 * 使用示例（假设 {@code haystack} 是包含 "ABCD" 的 {@link ByteBuf}，并且 {@code needles} 是 "AB"、"BC" 和 "CD"）：
 * <pre>
 *      MultiSearchProcessorFactory factory = MultiSearchProcessorFactory.newAhoCorasicSearchProcessorFactory(
 *          "AB".getBytes(CharsetUtil.UTF_8), "BC".getBytes(CharsetUtil.UTF_8), "CD".getBytes(CharsetUtil.UTF_8));
 *      MultiSearchProcessor processor = factory.newSearchProcessor();
 *
 *      int idx1 = haystack.forEachByte(processor);
 *      // idx1 是 1（"AB" 在 haystack 中出现的位置的最后一个字符的索引）
 *      // processor.getFoundNeedleId() 是 0（"AB" 在 needles[] 中的索引）
 *
 *      int continueFrom1 = idx1 + 1;
 *      // 从下一个字符继续搜索
 *
 *      int idx2 = haystack.forEachByte(continueFrom1, haystack.readableBytes() - continueFrom1, processor);
 *      // idx2 是 2（"BC" 在 haystack 中出现的位置的最后一个字符的索引）
 *      // processor.getFoundNeedleId() 是 1（"BC" 在 needles[] 中的索引）
 *
 *      int continueFrom2 = idx2 + 1;
 *
 *      int idx3 = haystack.forEachByte(continueFrom2, haystack.readableBytes() - continueFrom2, processor);
 *      // idx3 是 3（"CD" 在 haystack 中出现的位置的最后一个字符的索引）
 *      // processor.getFoundNeedleId() 是 2（"CD" 在 needles[] 中的索引）
 *
 *      int continueFrom3 = idx3 + 1;
 *
 *      int idx4 = haystack.forEachByte(continueFrom3, haystack.readableBytes() - continueFrom3, processor);
 *      // idx4 是 -1（没有更多的 needles 出现）
 *
 *      // 本次搜索会话完成，处理器应被丢弃。
 *      // 要再次搜索相同的 needles，请重用相同的 {@link AbstractMultiSearchProcessorFactory}
 *      // 以获取新的 MultiSearchProcessor。
 * </pre>
 */
public abstract class AbstractMultiSearchProcessorFactory implements MultiSearchProcessorFactory {

    /**
     * Creates a {@link MultiSearchProcessorFactory} based on
     * <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho–Corasick</a>
     * string search algorithm.
     * <br>
     * Precomputation (this method) time is linear in the size of input ({@code O(Σ|needles|)}).
     * <br>
     * The factory allocates and retains an array of 256 * X ints plus another array of X ints, where X
     * is the sum of lengths of each entry of {@code needles} minus the sum of lengths of repeated
     * prefixes of the {@code needles}.
     * <br>
     * Search (the actual application of {@link MultiSearchProcessor}) time is linear in the size of
     * {@link ByteBuf} on which the search is performed ({@code O(|haystack|)}).
     * Every byte of {@link ByteBuf} is processed only once, sequentually, regardles of
     * the number of {@code needles} being searched for.
     *
     * @param needles a varargs array of arrays of bytes to search for
     * @return a new instance of {@link AhoCorasicSearchProcessorFactory} precomputed for the given {@code needles}
     */

    /**
     * 基于<a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho–Corasick</a>
     * 字符串搜索算法创建一个{@link MultiSearchProcessorFactory}。
     * <br>
     * 预处理（此方法）的时间与输入的大小成线性关系（{@code O(Σ|needles|)}）。
     * <br>
     * 工厂分配并保留一个大小为256 * X的int数组和另一个大小为X的int数组，其中X
     * 是{@code needles}中每个条目长度的总和减去{@code needles}中重复前缀的长度总和。
     * <br>
     * 搜索（实际应用{@link MultiSearchProcessor}）的时间与执行搜索的{@link ByteBuf}的大小成线性关系
     * （{@code O(|haystack|)}）。
     * {@link ByteBuf}的每个字节仅被顺序处理一次，无论搜索的{@code needles}数量如何。
     *
     * @param needles 要搜索的字节数组的可变参数数组
     * @return 一个为给定{@code needles}预计算的{@link AhoCorasicSearchProcessorFactory}新实例
     */
    public static AhoCorasicSearchProcessorFactory newAhoCorasicSearchProcessorFactory(byte[] ...needles) {
        return new AhoCorasicSearchProcessorFactory(needles);
    }

}

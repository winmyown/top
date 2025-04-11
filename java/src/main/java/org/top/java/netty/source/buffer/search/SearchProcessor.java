package org.top.java.netty.source.buffer.search;

import io.netty.util.ByteProcessor;

/**
 * Interface for {@link ByteProcessor} that implements string search.
 * @see SearchProcessorFactory
 */

/**
 * 实现了字符串搜索的 {@link ByteProcessor} 接口。
 * @see SearchProcessorFactory
 */
public interface SearchProcessor extends ByteProcessor {

    /**
     * Resets the state of SearchProcessor.
     */

    /**
     * 重置SearchProcessor的状态。
     */
    void reset();

}

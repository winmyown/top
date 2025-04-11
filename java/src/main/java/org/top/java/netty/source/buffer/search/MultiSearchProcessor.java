package org.top.java.netty.source.buffer.search;

/**
 * Interface for {@link SearchProcessor} that implements simultaneous search for multiple strings.
 * @see MultiSearchProcessorFactory
 */

/**
 * {@link SearchProcessor} 接口，实现同时搜索多个字符串。
 * @see MultiSearchProcessorFactory
 */
public interface MultiSearchProcessor extends SearchProcessor {

    /**
     * @return the index of found search string (if any, or -1 if none) at current position of this MultiSearchProcessor
     */

    /**
     * @return 找到的搜索字符串的索引（如果有，如果没有则返回-1）在当前 MultiSearchProcessor 的位置
     */
    int getFoundNeedleId();

}

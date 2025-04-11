package org.top.java.netty.source.buffer.search;

public interface MultiSearchProcessorFactory extends SearchProcessorFactory {

    /**
     * Returns a new {@link MultiSearchProcessor}.
     */

    /**
     * 返回一个新的 {@link MultiSearchProcessor}。
     */
    @Override
    MultiSearchProcessor newSearchProcessor();

}

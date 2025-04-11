
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.concurrent.Future;
import org.top.java.netty.source.util.concurrent.Promise;

public interface AsyncMapping<IN, OUT> {

    /**
     * Returns the {@link Future} that will provide the result of the mapping. The given {@link Promise} will
     * be fulfilled when the result is available.
     */

    /**
     * 返回将提供映射结果的 {@link Future}。给定的 {@link Promise} 将在结果可用时被完成。
     */
    Future<OUT> map(IN input, Promise<OUT> promise);
}



package org.top.java.netty.source.util.concurrent;

/**
 * A subtype of {@link GenericFutureListener} that hides type parameter for convenience.
 * <pre>
 * Future f = new DefaultPromise(..);
 * f.addListener(new FutureListener() {
 *     public void operationComplete(Future f) { .. }
 * });
 * </pre>
 */

/**
 * {@link GenericFutureListener} 的一个子类型，为了方便隐藏了类型参数。
 * <pre>
 * Future f = new DefaultPromise(..);
 * f.addListener(new FutureListener() {
 *     public void operationComplete(Future f) { .. }
 * });
 * </pre>
 */
public interface FutureListener<V> extends GenericFutureListener<Future<V>> { }

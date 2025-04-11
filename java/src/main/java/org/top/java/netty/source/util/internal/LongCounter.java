
package org.top.java.netty.source.util.internal;

/**
 * Counter for long.
 */

/**
 * 长整型计数器。
 */
public interface LongCounter {
    void add(long delta);
    void increment();
    void decrement();
    long value();
}


package org.top.java.netty.source.util;

/**
 * Maintains the mapping from the objects of one type to the objects of the other type.
 */

/**
 * 维护一种类型的对象到另一种类型的对象的映射。
 */
public interface Mapping<IN, OUT> {

    /**
     * Returns mapped value of the specified input.
     */

    /**
     * 返回指定输入的映射值。
     */
    OUT map(IN input);
}

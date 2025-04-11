package org.top.java.netty.source.buffer.search;

import io.netty.util.internal.PlatformDependent;

/**
 * Implements
 * <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">Knuth-Morris-Pratt</a>
 * string search algorithm.
 * Use static {@link AbstractSearchProcessorFactory#newKmpSearchProcessorFactory}
 * to create an instance of this factory.
 * Use {@link KmpSearchProcessorFactory#newSearchProcessor} to get an instance of {@link io.netty.util.ByteProcessor}
 * implementation for performing the actual search.
 * @see AbstractSearchProcessorFactory
 */

/**
 * 实现
 * <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm">Knuth-Morris-Pratt</a>
 * 字符串搜索算法。
 * 使用静态方法 {@link AbstractSearchProcessorFactory#newKmpSearchProcessorFactory}
 * 来创建此工厂的实例。
 * 使用 {@link KmpSearchProcessorFactory#newSearchProcessor} 来获取 {@link io.netty.util.ByteProcessor}
 * 实现的实例，以执行实际的搜索。
 * @see AbstractSearchProcessorFactory
 */
public class KmpSearchProcessorFactory extends AbstractSearchProcessorFactory {

    private final int[] jumpTable;
    private final byte[] needle;

    public static class Processor implements SearchProcessor {

        private final byte[] needle;
        private final int[] jumpTable;
        private long currentPosition;

        Processor(byte[] needle, int[] jumpTable) {
            this.needle = needle;
            this.jumpTable = jumpTable;
        }

        @Override
        public boolean process(byte value) {
            while (currentPosition > 0 && PlatformDependent.getByte(needle, currentPosition) != value) {
                currentPosition = PlatformDependent.getInt(jumpTable, currentPosition);
            }
            if (PlatformDependent.getByte(needle, currentPosition) == value) {
                currentPosition++;
            }
            if (currentPosition == needle.length) {
                currentPosition = PlatformDependent.getInt(jumpTable, currentPosition);
                return false;
            }

            return true;
        }

        @Override
        public void reset() {
            currentPosition = 0;
        }
    }

    KmpSearchProcessorFactory(byte[] needle) {
        this.needle = needle.clone();
        this.jumpTable = new int[needle.length + 1];

        int j = 0;
        for (int i = 1; i < needle.length; i++) {
            while (j > 0 && needle[j] != needle[i]) {
                j = jumpTable[j];
            }
            if (needle[j] == needle[i]) {
                j++;
            }
            jumpTable[i + 1] = j;
        }
    }

    /**
     * Returns a new {@link Processor}.
     */

    /**
     * 返回一个新的 {@link Processor}。
     */
    @Override
    public Processor newSearchProcessor() {
        return new Processor(needle, jumpTable);
    }

}

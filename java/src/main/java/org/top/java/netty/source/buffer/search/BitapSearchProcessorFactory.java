package org.top.java.netty.source.buffer.search;

import io.netty.util.internal.PlatformDependent;

/**
 * Implements <a href="https://en.wikipedia.org/wiki/Bitap_algorithm">Bitap</a> string search algorithm.
 * Use static {@link AbstractSearchProcessorFactory#newBitapSearchProcessorFactory}
 * to create an instance of this factory.
 * Use {@link BitapSearchProcessorFactory#newSearchProcessor} to get an instance of {@link io.netty.util.ByteProcessor}
 * implementation for performing the actual search.
 * @see AbstractSearchProcessorFactory
 */

/**
 * 实现了<a href="https://en.wikipedia.org/wiki/Bitap_algorithm">Bitap</a>字符串搜索算法。
 * 使用静态方法 {@link AbstractSearchProcessorFactory#newBitapSearchProcessorFactory}
 * 来创建该工厂的实例。
 * 使用 {@link BitapSearchProcessorFactory#newSearchProcessor} 来获取 {@link io.netty.util.ByteProcessor}
 * 实现的实例，用于执行实际的搜索。
 * @see AbstractSearchProcessorFactory
 */
public class BitapSearchProcessorFactory extends AbstractSearchProcessorFactory {

    private final long[] bitMasks = new long[256];
    private final long successBit;

    public static class Processor implements SearchProcessor {

        private final long[] bitMasks;
        private final long successBit;
        private long currentMask;

        Processor(long[] bitMasks, long successBit) {
            this.bitMasks = bitMasks;
            this.successBit = successBit;
        }

        @Override
        public boolean process(byte value) {
            currentMask = ((currentMask << 1) | 1) & PlatformDependent.getLong(bitMasks, value & 0xffL);
            return (currentMask & successBit) == 0;
        }

        @Override
        public void reset() {
            currentMask = 0;
        }
    }

    BitapSearchProcessorFactory(byte[] needle) {
        if (needle.length > 64) {
            throw new IllegalArgumentException("Maximum supported search pattern length is 64, got " + needle.length);
        }

        long bit = 1L;
        for (byte c: needle) {
            bitMasks[c & 0xff] |= bit;
            bit <<= 1;
        }

        successBit = 1L << (needle.length - 1);
    }

    /**
     * Returns a new {@link Processor}.
     */

    /**
     * 返回一个新的 {@link Processor}。
     */
    @Override
    public Processor newSearchProcessor() {
        return new Processor(bitMasks, successBit);
    }

}

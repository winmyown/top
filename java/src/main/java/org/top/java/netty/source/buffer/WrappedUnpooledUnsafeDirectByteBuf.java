
package org.top.java.netty.source.buffer;

import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;

final class WrappedUnpooledUnsafeDirectByteBuf extends UnpooledUnsafeDirectByteBuf {

    WrappedUnpooledUnsafeDirectByteBuf(ByteBufAllocator alloc, long memoryAddress, int size, boolean doFree) {
        super(alloc, PlatformDependent.directBuffer(memoryAddress, size), size, doFree);
    }

    @Override
    protected void freeDirect(ByteBuffer buffer) {
        PlatformDependent.freeMemory(memoryAddress);
    }
}

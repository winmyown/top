/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * 根据 Apache 许可证 2.0 版本（“许可证”）授权;
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下网址获取许可证的副本：
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则按“原样”分发软件，
 * 没有任何明示或暗示的保证或条件。
 * 请参阅许可证以了解特定语言的权限和限制。
 */
package org.top.java.netty.source.util.internal.shaded.org.jctools.util;

/**
 * Power of 2 utility functions.
 */

/**
 * 2的幂次方工具函数。
 */
@InternalAPI
public final class Pow2 {
    public static final int MAX_POW2 = 1 << 30;

    /**
     * @param value from which next positive power of two will be found.
     * @return the next positive power of 2, this value if it is a power of 2. Negative values are mapped to 1.
     * @throws IllegalArgumentException is value is more than MAX_POW2 or less than 0
     */

    /**
     * @param value 从中找到下一个2的正幂的值。
     * @return 下一个2的正幂，如果该值已经是2的幂，则返回该值。负值将映射为1。
     * @throws IllegalArgumentException 如果值大于MAX_POW2或小于0
     */
    public static int roundToPowerOfTwo(final int value) {
        if (value > MAX_POW2) {
            throw new IllegalArgumentException("There is no larger power of 2 int for value:"+value+" since it exceeds 2^31.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Given value:"+value+". Expecting value >= 0.");
        }
        final int nextPow2 = 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
        return nextPow2;
    }

    /**
     * @param value to be tested to see if it is a power of two.
     * @return true if the value is a power of 2 otherwise false.
     */

    /**
     * @param 要测试的值，检查是否为2的幂。
     * @return 如果该值是2的幂则返回true，否则返回false。
     */
    public static boolean isPowerOfTwo(final int value) {
        return (value & (value - 1)) == 0;
    }

    /**
     * Align a value to the next multiple up of alignment. If the value equals an alignment multiple then it
     * is returned unchanged.
     *
     * @param value to be aligned up.
     * @param alignment to be used, must be a power of 2.
     * @return the value aligned to the next boundary.
     */

    /**
     * 将值对齐到下一个对齐倍数的边界。如果值已经是对齐倍数的边界，则返回原值。
     *
     * @param value 需要对齐的值。
     * @param alignment 使用的对齐值，必须是2的幂。
     * @return 对齐到下一个边界的值。
     */
    public static long align(final long value, final int alignment) {
        if (!isPowerOfTwo(alignment)) {
            throw new IllegalArgumentException("alignment must be a power of 2:" + alignment);
        }
        return (value + (alignment - 1)) & ~(alignment - 1);
    }
}

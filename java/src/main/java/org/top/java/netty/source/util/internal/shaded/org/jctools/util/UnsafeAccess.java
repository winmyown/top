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

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Why should we resort to using Unsafe?<br>
 * <ol>
 * <li>To construct class fields which allow volatile/ordered/plain access: This requirement is covered by
 * {@link AtomicReferenceFieldUpdater} and similar but their performance is arguably worse than the DIY approach
 * (depending on JVM version) while Unsafe intrinsification is a far lesser challenge for JIT compilers.
 * <li>To construct flavors of {@link AtomicReferenceArray}.
 * <li>Other use cases exist but are not present in this library yet.
 * </ol>
 *
 * @author nitsanw
 */

/**
 * 为什么要使用 Unsafe？<br>
 * <ol>
 * <li>为了构造允许 volatile/ordered/plain 访问的类字段：这一需求可以通过
 * {@link AtomicReferenceFieldUpdater} 及其类似类来满足，但它们的性能可能比 DIY 方法更差（取决于 JVM 版本），而 Unsafe 的内联化对 JIT 编译器来说是一个更小的挑战。
 * <li>为了构造 {@link AtomicReferenceArray} 的变体。
 * <li>其他用例也存在，但尚未出现在此库中。
 * </ol>
 *
 * @author nitsanw
 */
@InternalAPI
public class UnsafeAccess
{
    public static final boolean SUPPORTS_GET_AND_SET_REF;
    public static final boolean SUPPORTS_GET_AND_ADD_LONG;
    public static final Unsafe UNSAFE;

    static
    {
        UNSAFE = getUnsafe();
        SUPPORTS_GET_AND_SET_REF = hasGetAndSetSupport();
        SUPPORTS_GET_AND_ADD_LONG = hasGetAndAddLongSupport();
    }

    private static Unsafe getUnsafe()
    {
        Unsafe instance;
        try
        {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            instance = (Unsafe) field.get(null);
        }
        catch (Exception ignored)
        {
            // Some platforms, notably Android, might not have a sun.misc.Unsafe implementation with a private
            // 某些平台，特别是 Android，可能没有带有私有的 sun.misc.Unsafe 实现
            // `theUnsafe` static instance. In this case we can try to call the default constructor, which is sufficient
            // `theUnsafe` 静态实例。在这种情况下，我们可以尝试调用默认构造函数，这已经足够
            // for Android usage.
            // 对于 Android 使用。
            try
            {
                Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private static boolean hasGetAndSetSupport()
    {
        try
        {
            Unsafe.class.getMethod("getAndSetObject", Object.class, Long.TYPE, Object.class);
            return true;
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    private static boolean hasGetAndAddLongSupport()
    {
        try
        {
            Unsafe.class.getMethod("getAndAddLong", Object.class, Long.TYPE, Long.TYPE);
            return true;
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    public static long fieldOffset(Class clz, String fieldName) throws RuntimeException
    {
        try
        {
            return UNSAFE.objectFieldOffset(clz.getDeclaredField(fieldName));
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }
}

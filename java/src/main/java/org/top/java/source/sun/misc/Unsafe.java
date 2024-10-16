package org.top.java.source.sun.misc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午4:47
 */
public final class Unsafe {
    // 单例对象，系统中唯一的 Unsafe 实例
    private static final Unsafe theUnsafe;

    // 表示无效字段偏移量的常量
    public static final int INVALID_FIELD_OFFSET = -1;

    // 数组类型的基础偏移量和索引比例
    public static final int ARRAY_BOOLEAN_BASE_OFFSET;
    public static final int ARRAY_BYTE_BASE_OFFSET;
    public static final int ARRAY_SHORT_BASE_OFFSET;
    public static final int ARRAY_CHAR_BASE_OFFSET;
    public static final int ARRAY_INT_BASE_OFFSET;
    public static final int ARRAY_LONG_BASE_OFFSET;
    public static final int ARRAY_FLOAT_BASE_OFFSET;
    public static final int ARRAY_DOUBLE_BASE_OFFSET;
    public static final int ARRAY_OBJECT_BASE_OFFSET;

    // 数组类型的索引比例
    public static final int ARRAY_BOOLEAN_INDEX_SCALE;
    public static final int ARRAY_BYTE_INDEX_SCALE;
    public static final int ARRAY_SHORT_INDEX_SCALE;
    public static final int ARRAY_CHAR_INDEX_SCALE;
    public static final int ARRAY_INT_INDEX_SCALE;
    public static final int ARRAY_LONG_INDEX_SCALE;
    public static final int ARRAY_FLOAT_INDEX_SCALE;
    public static final int ARRAY_DOUBLE_INDEX_SCALE;
    public static final int ARRAY_OBJECT_INDEX_SCALE;

    // 系统地址大小
    public static final int ADDRESS_SIZE;

    // 注册本地方法
    private static native void registerNatives();

    // 私有化构造函数，防止外部创建实例
    private Unsafe() {
    }

    // 获取 Unsafe 实例，只允许在特定系统类加载器环境下调用
    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class var0 = Reflection.getCallerClass();
        // 检查调用者的类加载器是否是系统类加载器
        if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
            throw new SecurityException("Unsafe");
        } else {
            return theUnsafe;
        }
    }

    // 原生方法：从指定对象的指定内存偏移量获取 int 类型值
    public native int getInt(Object var1, long var2);

    // 原生方法：将 int 类型值写入指定对象的指定内存偏移量
    public native void putInt(Object var1, long var2, int var4);

    // 原生方法：从指定对象的指定内存偏移量获取对象
    public native Object getObject(Object var1, long var2);

    // 原生方法：将对象写入指定对象的指定内存偏移量
    public native void putObject(Object var1, long var2, Object var4);

    // 原生方法：从指定对象的指定内存偏移量获取 boolean 类型值
    public native boolean getBoolean(Object var1, long var2);

    // 原生方法：将 boolean 类型值写入指定对象的指定内存偏移量
    public native void putBoolean(Object var1, long var2, boolean var4);

    // 原生方法：从指定对象的指定内存偏移量获取 byte 类型值
    public native byte getByte(Object var1, long var2);

    // 原生方法：将 byte 类型值写入指定对象的指定内存偏移量
    public native void putByte(Object var1, long var2, byte var4);

    // 原生方法：从指定对象的指定内存偏移量获取 short 类型值
    public native short getShort(Object var1, long var2);

    // 原生方法：将 short 类型值写入指定对象的指定内存偏移量
    public native void putShort(Object var1, long var2, short var4);

    // 原生方法：从指定对象的指定内存偏移量获取 char 类型值
    public native char getChar(Object var1, long var2);

    // 原生方法：将 char 类型值写入指定对象的指定内存偏移量
    public native void putChar(Object var1, long var2, char var4);

    // 原生方法：从指定对象的指定内存偏移量获取 long 类型值
    public native long getLong(Object var1, long var2);

    // 原生方法：将 long 类型值写入指定对象的指定内存偏移量
    public native void putLong(Object var1, long var2, long var4);

    // 原生方法：从指定对象的指定内存偏移量获取 float 类型值
    public native float getFloat(Object var1, long var2);

    // 原生方法：将 float 类型值写入指定对象的指定内存偏移量
    public native void putFloat(Object var1, long var2, float var4);

    // 原生方法：从指定对象的指定内存偏移量获取 double 类型值
    public native double getDouble(Object var1, long var2);

    // 原生方法：将 double 类型值写入指定对象的指定内存偏移量
    public native void putDouble(Object var1, long var2, double var4);

    // 下面是已弃用的方法的包装，允许通过 int 偏移量进行访问
    @Deprecated
    public int getInt(Object var1, int var2) {
        return this.getInt(var1, (long)var2);
    }

    @Deprecated
    public void putInt(Object var1, int var2, int var3) {
        this.putInt(var1, (long)var2, var3);
    }

    @Deprecated
    public Object getObject(Object var1, int var2) {
        return this.getObject(var1, (long)var2);
    }

    @Deprecated
    public void putObject(Object var1, int var2, Object var3) {
        this.putObject(var1, (long)var2, var3);
    }

    @Deprecated
    public boolean getBoolean(Object var1, int var2) {
        return this.getBoolean(var1, (long)var2);
    }

    @Deprecated
    public void putBoolean(Object var1, int var2, boolean var3) {
        this.putBoolean(var1, (long)var2, var3);
    }

    @Deprecated
    public byte getByte(Object var1, int var2) {
        return this.getByte(var1, (long)var2);
    }

    @Deprecated
    public void putByte(Object var1, int var2, byte var3) {
        this.putByte(var1, (long)var2, var3);
    }

    @Deprecated
    public short getShort(Object var1, int var2) {
        return this.getShort(var1, (long)var2);
    }

    @Deprecated
    public void putShort(Object var1, int var2, short var3) {
        this.putShort(var1, (long)var2, var3);
    }

    @Deprecated
    public char getChar(Object var1, int var2) {
        return this.getChar(var1, (long)var2);
    }

    @Deprecated
    public void putChar(Object var1, int var2, char var3) {
        this.putChar(var1, (long)var2, var3);
    }

    @Deprecated
    public long getLong(Object var1, int var2) {
        return this.getLong(var1, (long)var2);
    }

    @Deprecated
    public void putLong(Object var1, int var2, long var3) {
        this.putLong(var1, (long)var2, var3);
    }

    @Deprecated
    public float getFloat(Object var1, int var2) {
        return this.getFloat(var1, (long)var2);
    }

    @Deprecated
    public void putFloat(Object var1, int var2, float var3) {
        this.putFloat(var1, (long)var2, var3);
    }

    @Deprecated
    public double getDouble(Object var1, int var2) {
        return this.getDouble(var1, (long)var2);
    }

    @Deprecated
    public void putDouble(Object var1, int var2, double var3) {
        this.putDouble(var1, (long)var2, var3);
    }

    // 原生方法：获取指定内存地址的 byte 值
    public native byte getByte(long var1);

    // 原生方法：将 byte 值写入指定内存地址
    public native void putByte(long var1, byte var3);

    // 原生方法：获取指定内存地址的 short 值
    public native short getShort(long var1);

    // 原生方法：将 short 值写入指定内存地址
    public native void putShort(long var1, short var3);

    // 原生方法：获取指定内存地址的 char 值
    public native char getChar(long var1);

    // 原生方法：将 char 值写入指定内存地址
    public native void putChar(long var1, char var3);

    // 原生方法：获取指定内存地址的 int 值
    public native int getInt(long var1);

    // 原生方法：将 int 值写入指定内存地址
    public native void putInt(long var1, int var3);

    // 原生方法：获取指定内存地址的 long 值
    public native long getLong(long var1);

    // 原生方法：将 long 值写入指定内存地址
    public native void putLong(long var1, long var3);

    // 原生方法：获取指定内存地址的 float 值
    public native float getFloat(long var1);

    // 原生方法：将 float 值写入指定内存地址
    public native void putFloat(long var1, float var3);

    // 原生方法：获取指定内存地址的 double 值
    public native double getDouble(long var1);

    // 原生方法：将 double 值写入指定内存地址
    public native void putDouble(long var1, double var3);

    // 原生方法：获取指定内存地址的指针
    public native long getAddress(long var1);

    // 原生方法：将指针值写入指定内存地址
    public native void putAddress(long var1, long var3);

    // 原生方法：分配指定大小的内存，返回指向该内存的地址
    public native long allocateMemory(long var1);

    // 原生方法：重新分配指定大小的内存，返回指向新内存的地址
    public native long reallocateMemory(long var1, long var3);

    // 原生方法：设置指定内存块为指定的值
    public native void setMemory(Object var1, long var2, long var4, byte var6);

    // 设置指定地址的内存块为指定的值
    public void setMemory(long var1, long var3, byte var5) {
        this.setMemory((Object)null, var1, var3, var5);
    }

    // 原生方法：将一个内存区域的数据复制到另一个内存区域
    public native void copyMemory(Object var1, long var2, Object var4, long var5, long var7);

    // 复制内存块
    public void copyMemory(long var1, long var3, long var5) {
        this.copyMemory((Object)null, var1, (Object)null, var3, var5);
    }

    // 原生方法：释放指定的内存地址
    public native void freeMemory(long var1);

    // 获取字段偏移量（已弃用方法）
    @Deprecated
    public int fieldOffset(Field var1) {
        return Modifier.isStatic(var1.getModifiers()) ? (int)this.staticFieldOffset(var1) : (int)this.objectFieldOffset(var1);
    }

    // 获取类的静态字段的基地址（已弃用方法）
    @Deprecated
    public Object staticFieldBase(Class<?> var1) {
        Field[] var2 = var1.getDeclaredFields();

        for(int var3 = 0; var3 < var2.length; ++var3) {
            if (Modifier.isStatic(var2[var3].getModifiers())) {
                return this.staticFieldBase(var2[var3]);
            }
        }

        return null;
    }

    // 原生方法：获取静态字段的偏移量
    public native long staticFieldOffset(Field var1);

    // 原生方法：获取实例字段的偏移量
    public native long objectFieldOffset(Field var1);

    // 原生方法：获取静态字段的基地址
    public native Object staticFieldBase(Field var1);

    // 原生方法：检查类是否需要初始化
    public native boolean shouldBeInitialized(Class<?> var1);

    // 原生方法：确保类已被初始化
    public native void ensureClassInitialized(Class<?> var1);

    // 原生方法：获取数组的基础偏移量
    public native int arrayBaseOffset(Class<?> var1);

    // 原生方法：获取数组的索引比例
    public native int arrayIndexScale(Class<?> var1);

    // 原生方法：获取地址的大小
    public native int addressSize();

    // 原生方法：获取页面大小
    public native int pageSize();

    // 原生方法：定义一个新的类
    public native Class<?> defineClass(String var1, byte[] var2, int var3, int var4, ClassLoader var5, ProtectionDomain var6);

    // 原生方法：定义一个匿名类
    public native Class<?> defineAnonymousClass(Class<?> var1, byte[] var2, Object[] var3);

    // 原生方法：为类分配一个实例
    public native Object allocateInstance(Class<?> var1) throws InstantiationException;

    // 监视器相关操作（已弃用）
    @Deprecated
    public native void monitorEnter(Object var1);

    @Deprecated
    public native void monitorExit(Object var1);

    @Deprecated
    public native boolean tryMonitorEnter(Object var1);

    // 原生方法：抛出异常
    public native void throwException(Throwable var1);

    // 比较并交换对象
    public final native boolean compareAndSwapObject(Object var1, long var2, Object var4, Object var5);

    // 比较并交换整数
    public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);

    // 比较并交换长整数
    public final native boolean compareAndSwapLong(Object var1, long var2, long var4, long var6);

    // 获取 volatile 类型的对象
    public native Object getObjectVolatile(Object var1, long var2);

    // 设置 volatile 类型的对象
    public native void putObjectVolatile(Object var1, long var2, Object var4);

    // 获取 volatile 类型的 int
    public native int getIntVolatile(Object var1, long var2);

    // 设置 volatile 类型的 int
    public native void putIntVolatile(Object var1, long var2, int var4);

    // 获取 volatile 类型的 boolean
    public native boolean getBooleanVolatile(Object var1, long var2);

    // 设置 volatile 类型的 boolean
    public native void putBooleanVolatile(Object var1, long var2, boolean var4);

    // 获取 volatile 类型的 byte
    public native byte getByteVolatile(Object var1, long var2);

    // 设置 volatile 类型的 byte
    public native void putByteVolatile(Object var1, long var2, byte var4);

    // 获取 volatile 类型的 short
    public native short getShortVolatile(Object var1, long var2);

    // 设置 volatile 类型的 short
    public native void putShortVolatile(Object var1, long var2, short var4);

    // 获取 volatile 类型的 char
    public native char getCharVolatile(Object var1, long var2);

    // 设置 volatile 类型的 char
    public native void putCharVolatile(Object var1, long var2, char var4);

    // 获取 volatile 类型的 long
    public native long getLongVolatile(Object var1, long var2);

    // 设置 volatile 类型的 long
    public native void putLongVolatile(Object var1, long var2, long var4);

    // 获取 volatile 类型的 float
    public native float getFloatVolatile(Object var1, long var2);

    // 设置 volatile 类型的 float
    public native void putFloatVolatile(Object var1, long var2, float var4);

    // 获取 volatile 类型的 double
    public native double getDoubleVolatile(Object var1, long var2);

    // 设置 volatile 类型的 double
    public native void putDoubleVolatile(Object var1, long var2, double var4);

    // 设置有序的对象
    public native void putOrderedObject(Object var1, long var2, Object var4);

    // 设置有序的 int
    public native void putOrderedInt(Object var1, long var2, int var4);

    // 设置有序的 long
    public native void putOrderedLong(Object var1, long var2, long var4);

    // 取消阻塞某个线程
    public native void unpark(Object var1);

    // 阻塞当前线程
    public native void park(boolean var1, long var2);

    // 获取负载平均值
    public native int getLoadAverage(double[] var1, int var2);

    // 获取并增加 int 值
    public final int getAndAddInt(Object var1, long var2, int var4) {
        int var5;
        do {
            var5 = this.getIntVolatile(var1, var2);
        } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));

        return var5;
    }

    // 获取并增加 long 值
    public final long getAndAddLong(Object var1, long var2, long var4) {
        long var6;
        do {
            var6 = this.getLongVolatile(var1, var2);
        } while(!this.compareAndSwapLong(var1, var2, var6, var6 + var4));

        return var6;
    }

    // 获取并设置 int 值
    public final int getAndSetInt(Object var1, long var2, int var4) {
        int var5;
        do {
            var5 = this.getIntVolatile(var1, var2);
        } while(!this.compareAndSwapInt(var1, var2, var5, var4));

        return var5;
    }

    // 获取并设置 long 值
    public final long getAndSetLong(Object var1, long var2, long var4) {
        long var6;
        do {
            var6 = this.getLongVolatile(var1, var2);
        } while(!this.compareAndSwapLong(var1, var2, var6, var4));

        return var6;
    }

    // 获取并设置对象
    public final Object getAndSetObject(Object var1, long var2, Object var4) {
        Object var5;
        do {
            var5 = this.getObjectVolatile(var1, var2);
        } while(!this.compareAndSwapObject(var1, var2, var5, var4));

        return var5;
    }

    // 加载内存屏障
    public native void loadFence();

    // 存储内存屏障
    public native void storeFence();

    // 完全内存屏障
    public native void fullFence();

    // 抛出非法访问错误
    private static void throwIllegalAccessError() {
        throw new IllegalAccessError();
    }

    // 初始化 Unsafe 实例以及相关常量
    static {
        registerNatives(); // 注册本地方法
        Reflection.registerMethodsToFilter(Unsafe.class, new String[]{"getUnsafe"});
        theUnsafe = new Unsafe(); // 初始化单例 Unsafe 实例

        // 初始化数组相关的常量
        ARRAY_BOOLEAN_BASE_OFFSET = theUnsafe.arrayBaseOffset(boolean[].class);
        ARRAY_BYTE_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);
        ARRAY_SHORT_BASE_OFFSET = theUnsafe.arrayBaseOffset(short[].class);
        ARRAY_CHAR_BASE_OFFSET = theUnsafe.arrayBaseOffset(char[].class);
        ARRAY_INT_BASE_OFFSET = theUnsafe.arrayBaseOffset(int[].class);
        ARRAY_LONG_BASE_OFFSET = theUnsafe.arrayBaseOffset(long[].class);
        ARRAY_FLOAT_BASE_OFFSET = theUnsafe.arrayBaseOffset(float[].class);
        ARRAY_DOUBLE_BASE_OFFSET = theUnsafe.arrayBaseOffset(double[].class);
        ARRAY_OBJECT_BASE_OFFSET = theUnsafe.arrayBaseOffset(Object[].class);

        ARRAY_BOOLEAN_INDEX_SCALE = theUnsafe.arrayIndexScale(boolean[].class);
        ARRAY_BYTE_INDEX_SCALE = theUnsafe.arrayIndexScale(byte[].class);
        ARRAY_SHORT_INDEX_SCALE = theUnsafe.arrayIndexScale(short[].class);
        ARRAY_CHAR_INDEX_SCALE = theUnsafe.arrayIndexScale(char[].class);
        ARRAY_INT_INDEX_SCALE = theUnsafe.arrayIndexScale(int[].class);
        ARRAY_LONG_INDEX_SCALE = theUnsafe.arrayIndexScale(long[].class);
        ARRAY_FLOAT_INDEX_SCALE = theUnsafe.arrayIndexScale(float[].class);
        ARRAY_DOUBLE_INDEX_SCALE = theUnsafe.arrayIndexScale(double[].class);
        ARRAY_OBJECT_INDEX_SCALE = theUnsafe.arrayIndexScale(Object[].class);

        // 初始化地址大小
        ADDRESS_SIZE = theUnsafe.addressSize();
    }
}



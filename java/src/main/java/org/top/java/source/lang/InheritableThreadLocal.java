package org.top.java.source.lang;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午4:58
 */
/**
 * 此类扩展了 <tt>ThreadLocal</tt>，提供了从父线程继承值到子线程的功能：
 * 当创建子线程时，子线程将继承所有可继承的线程局部变量的初始值（如果父线程具有相应的值）。
 * 通常，子线程的值与父线程的值是相同的；然而，可以通过覆盖此类中的 <tt>childValue</tt> 方法来使子线程的值成为父线程值的任意函数。
 *
 * <p>当维护的每线程属性（例如用户 ID，事务 ID）需要自动传递给任何创建的子线程时，优先使用可继承的线程局部变量，而非普通的线程局部变量。
 *
 * @author  Josh Bloch 和 Doug Lea
 * @see     ThreadLocal
 * @since   1.2
 */
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * 计算子线程初始的线程局部变量值，该值是根据创建子线程时父线程的值得出的。
     * <p>
     * 此方法在父线程中调用，在子线程启动之前执行。
     * <p>
     * 该方法仅返回输入参数，如果需要不同的行为，可以覆盖此方法。
     *
     * @param parentValue 父线程的值
     * @return 子线程的初始值
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     * 获取与 ThreadLocal 关联的映射表。
     *
     * @param t 当前线程
     */
    ThreadLocalMap getMap(Thread t) {
        return t.inheritableThreadLocals;
    }

    /**
     * 创建与 ThreadLocal 关联的映射表。
     *
     * @param t 当前线程
     * @param firstValue 映射表中的初始值
     */
    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}



package org.top.java.netty.source.util;


/**
 * A special {@link Error} which is used to signal some state or request by throwing it.
 * {@link Signal} has an empty stack trace and has no cause to save the instantiation overhead.
 */


/**
 * 一种特殊的 {@link Error}，用于通过抛出它来发出某些状态或请求的信号。
 * {@link Signal} 有一个空的堆栈跟踪，并且没有原因以节省实例化开销。
 */
public final class Signal extends Error implements Constant<Signal> {

    private static final long serialVersionUID = -221145131122459977L;

    private static final ConstantPool<Signal> pool = new ConstantPool<Signal>() {
        @Override
        protected Signal newConstant(int id, String name) {
            return new Signal(id, name);
        }
    };

    /**
     * Returns the {@link Signal} of the specified name.
     */

    /**
     * 返回指定名称的 {@link Signal}。
     */
    public static Signal valueOf(String name) {
        return pool.valueOf(name);
    }

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */

    /**
     * {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)} 的快捷方式。
     */
    public static Signal valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return pool.valueOf(firstNameComponent, secondNameComponent);
    }

    private final SignalConstant constant;

    /**
     * Creates a new {@link Signal} with the specified {@code name}.
     */

    /**
     * 创建一个具有指定 {@code name} 的新 {@link Signal}。
     */
    private Signal(int id, String name) {
        constant = new SignalConstant(id, name);
    }

    /**
     * Check if the given {@link Signal} is the same as this instance. If not an {@link IllegalStateException} will
     * be thrown.
     */

    /**
     * 检查给定的 {@link Signal} 是否与此实例相同。如果不相同，将抛出 {@link IllegalStateException}。
     */
    public void expect(Signal signal) {
        if (this != signal) {
            throw new IllegalStateException("unexpected signal: " + signal);
        }
    }

    // Suppress a warning since the method doesn't need synchronization

    // 抑制警告，因为该方法不需要同步
    @Override
    public Throwable initCause(Throwable cause) {   // lgtm[java/non-sync-override]
        return this;
    }

    // Suppress a warning since the method doesn't need synchronization

    // 抑制警告，因为该方法不需要同步
    @Override
    public Throwable fillInStackTrace() {   // lgtm[java/non-sync-override]
        return this;
    }

    @Override
    public int id() {
        return constant.id();
    }

    @Override
    public String name() {
        return constant.name();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public int compareTo(Signal other) {
        if (this == other) {
            return 0;
        }

        return constant.compareTo(other.constant);
    }

    @Override
    public String toString() {
        return name();
    }

    private static final class SignalConstant extends AbstractConstant<SignalConstant> {
        SignalConstant(int id, String name) {
            super(id, name);
        }
    }
}

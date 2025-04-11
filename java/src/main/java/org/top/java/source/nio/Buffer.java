/*
 * 版权所有 (c) 2000, 2013, Oracle 和/或其附属公司。保留所有权利。
 * ORACLE 专有/机密。使用受许可条款约束。
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.top.java.source.nio;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import java.util.Spliterator;

/**
 * 用于特定基本类型数据的容器。
 *
 * <p> 缓冲区是特定基本类型元素的线性、有限序列。除了其内容外，缓冲区的基本属性是其容量、限制和位置：</p>
 *
 * <blockquote>
 *
 *   <p> 缓冲区的<i>容量</i>是它包含的元素数量。缓冲区的容量永远不会为负，也永远不会改变。</p>
 *
 *   <p> 缓冲区的<i>限制</i>是不应读取或写入的第一个元素的索引。缓冲区的限制永远不会为负，也永远不会大于其容量。</p>
 *
 *   <p> 缓冲区的<i>位置</i>是下一个要读取或写入的元素的索引。缓冲区的位置永远不会为负，也永远不会大于其限制。</p>
 *
 * </blockquote>
 *
 * <p> 对于每个非布尔基本类型，都有一个此类的子类。
 *
 *
 * <h2> 数据传输 </h2>
 *
 * <p> 此类的每个子类定义了两类<i>get</i>和<i>put</i>操作：</p>
 *
 * <blockquote>
 *
 *   <p> <i>相对</i>操作从当前位置开始读取或写入一个或多个元素，然后将位置增加传输的元素数量。如果请求的传输超出限制，则相对<i>get</i>操作会抛出 {@link BufferUnderflowException}，而相对<i>put</i>操作会抛出 {@link BufferOverflowException}；在这两种情况下，都不会传输数据。</p>
 *
 *   <p> <i>绝对</i>操作显式指定元素索引，并且不影响位置。如果索引参数超出限制，绝对<i>get</i>和<i>put</i>操作会抛出 {@link IndexOutOfBoundsException}。</p>
 *
 * </blockquote>
 *
 * <p> 当然，数据也可以通过适当通道的I/O操作传输到缓冲区或从缓冲区传输出去，这些操作始终相对于当前位置。
 *
 *
 * <h2> 标记和重置 </h2>
 *
 * <p> 缓冲区的<i>标记</i>是调用 {@link #reset reset} 方法时位置将被重置到的索引。标记并不总是定义，但当它定义时，它永远不会为负，也永远不会大于位置。如果标记已定义，则当位置或限制调整到小于标记的值时，标记将被丢弃。如果标记未定义，则调用 {@link #reset reset} 方法会抛出 {@link InvalidMarkException}。
 *
 *
 * <h2> 不变量 </h2>
 *
 * <p> 以下不变量适用于标记、位置、限制和容量值：
 *
 * <blockquote>
 *     <tt>0</tt> <tt>&lt;=</tt>
 *     <i>mark</i> <tt>&lt;=</tt>
 *     <i>position</i> <tt>&lt;=</tt>
 *     <i>limit</i> <tt>&lt;=</tt>
 *     <i>capacity</i>
 * </blockquote>
 *
 * <p> 新创建的缓冲区的位置始终为零，标记未定义。初始限制可能为零，也可能是其他值，具体取决于缓冲区的类型及其构造方式。新分配的缓冲区的每个元素都初始化为零。
 *
 *
 * <h2> 清除、翻转和倒带 </h2>
 *
 * <p> 除了访问位置、限制和容量值以及标记和重置的方法外，此类还定义了以下对缓冲区的操作：
 *
 * <ul>
 *
 *   <li><p> {@link #clear} 使缓冲区准备好进行新的通道读取或相对<i>put</i>操作：它将限制设置为容量，并将位置设置为零。</p></li>
 *
 *   <li><p> {@link #flip} 使缓冲区准备好进行新的通道写入或相对<i>get</i>操作：它将限制设置为当前位置，然后将位置设置为零。</p></li>
 *
 *   <li><p> {@link #rewind} 使缓冲区准备好重新读取它已经包含的数据：它保持限制不变，并将位置设置为零。</p></li>
 *
 * </ul>
 *
 *
 * <h2> 只读缓冲区 </h2>
 *
 * <p> 每个缓冲区都是可读的，但并非每个缓冲区都是可写的。每个缓冲区类的修改方法被指定为<i>可选操作</i>，当在只读缓冲区上调用时，会抛出 {@link ReadOnlyBufferException}。只读缓冲区不允许更改其内容，但其标记、位置和限制值是可变的。可以通过调用 {@link #isReadOnly isReadOnly} 方法来确定缓冲区是否为只读。
 *
 *
 * <h2> 线程安全 </h2>
 *
 * <p> 缓冲区对于多个并发线程的使用是不安全的。如果缓冲区将由多个线程使用，则应通过适当的同步控制对缓冲区的访问。
 *
 *
 * <h2> 调用链 </h2>
 *
 * <p> 此类中没有其他返回值的方法被指定为返回它们被调用的缓冲区。这允许方法调用被链接；例如，语句序列
 *
 * <blockquote><pre>
 * b.flip();
 * b.position(23);
 * b.limit(42);</pre></blockquote>
 *
 * 可以被更紧凑的单条语句替换
 *
 * <blockquote><pre>
 * b.flip().position(23).limit(42);</pre></blockquote>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class Buffer {

    /**
     * 遍历和分割存储在缓冲区中的元素的Spliterators的特性。
     */
    static final int SPLITERATOR_CHARACTERISTICS =
        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;

    // 不变量：mark <= position <= limit <= capacity
    private int mark = -1;
    private int position = 0;
    private int limit;
    private int capacity;

    // 仅由直接缓冲区使用
    // 注意：为了提高 JNI GetDirectBufferAddress 的速度，这里进行了提升
    long address;

    // 使用给定的标记、位置、限制和容量创建一个新的缓冲区
    // 在检查不变量之后。
    //  将这些java注释以原格式翻译，保留注释字符。仅输出翻译结果，无需解释，无需注释。
    Buffer(int mark, int pos, int lim, int cap) {       // 包私有
        if (cap < 0)
            throw new IllegalArgumentException("Negative capacity: " + cap);
        this.capacity = cap;
        limit(lim);
        position(pos);
        if (mark >= 0) {
            if (mark > pos)
                throw new IllegalArgumentException("mark > position: ("
                                                   + mark + " > " + pos + ")");
            this.mark = mark;
        }
    }

    /**
     * 返回此缓冲区的容量。
     *
     * @return  此缓冲区的容量
     */
    public final int capacity() {
        return capacity;
    }

    /**
     * 返回此缓冲区的当前位置。
     *
     * @return  此缓冲区的位置
     */
    public final int position() {
        return position;
    }

    /**
     * 设置此缓冲区的位置。如果标记已定义且大于新位置，则将其丢弃。
     *
     * @param  newPosition
     *         新的位置值；必须为非负数
     *         且不大于当前限制
     *
     * @return  此缓冲区
     *
     * @throws  IllegalArgumentException
     *          如果 <tt>newPosition</tt> 的前置条件不成立
     */
    public final Buffer position(int newPosition) {
        if ((newPosition > limit) || (newPosition < 0))
            throw new IllegalArgumentException();
        position = newPosition;
        if (mark > position) mark = -1;
        return this;
    }

    /**
     * 返回此缓冲区的限制。
     *
     * @return  此缓冲区的限制
     */
    public final int limit() {
        return limit;
    }

    /**
     * 设置此缓冲区的限制。如果当前位置大于新的限制，
     * 则将其设置为新的限制。如果标记已定义且大于新的限制，
     * 则将其丢弃。
     *
     * @param  newLimit
     *         新的限制值；必须为非负数
     *         且不得大于此缓冲区的容量
     *
     * @return  此缓冲区
     *
     * @throws  IllegalArgumentException
     *          如果 <tt>newLimit</tt> 的前提条件不成立
     */
    public final Buffer limit(int newLimit) {
        if ((newLimit > capacity) || (newLimit < 0))
            throw new IllegalArgumentException();
        limit = newLimit;
        if (position > limit) position = limit;
        if (mark > limit) mark = -1;
        return this;
    }

    /**
     * 将此缓冲区的标记设置为其当前位置。
     *
     * @return  此缓冲区
     */
    public final Buffer mark() {
        mark = position;
        return this;
    }

    /**
     * 将此缓冲区的位置重置为之前标记的位置。
     *
     * <p> 调用此方法既不会更改也不会丢弃标记的值。 </p>
     *
     * @return  此缓冲区
     *
     * @throws  InvalidMarkException
     *          如果尚未设置标记
     */
    public final Buffer reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }

    /**
     * 清除此缓冲区。将位置设置为零，将限制设置为容量，并丢弃标记。
     *
     * <p> 在使用一系列通道读取或 <i>put</i> 操作填充此缓冲区之前调用此方法。例如：
     *
     * <blockquote><pre>
     * buf.clear();     // 准备缓冲区进行读取
     * in.read(buf);    // 读取数据</pre></blockquote>
     *
     * <p> 此方法实际上并不会清除缓冲区中的数据，但之所以如此命名，是因为它通常用于这种情况。
     *
     * @return  此缓冲区
     */
    public final Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    /**
     * 翻转此缓冲区。将限制设置为当前位置，然后将位置设置为零。如果定义了标记，则将其丢弃。
     *
     * <p> 在一系列通道读取或<i>put</i>操作之后，调用此方法以准备进行一系列通道写入或相对<i>get</i>操作。例如：
     *
     * <blockquote><pre>
     * buf.put(magic);    // 添加头部
     * in.read(buf);      // 将数据读取到缓冲区的其余部分
     * buf.flip();        // 翻转缓冲区
     * out.write(buf);    // 将头部 + 数据写入通道</pre></blockquote>
     *
     * <p> 此方法通常与 {@link ByteBuffer#compact compact} 方法结合使用，用于将数据从一个地方传输到另一个地方。 </p>
     *
     * @return  此缓冲区
     */
    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 重绕此缓冲区。将位置设置为零并丢弃标记。
     *
     * <p> 在通道写入或 <i>get</i> 操作序列之前调用此方法，假设限制已经适当地设置。例如：
     *
     * <blockquote><pre>
     * out.write(buf);    // 写入剩余数据
     * buf.rewind();      // 重绕缓冲区
     * buf.get(array);    // 将数据复制到数组中</pre></blockquote>
     *
     * @return  此缓冲区
     */
    public final Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 返回当前位置与限制之间的元素数量。
     *
     * @return  此缓冲区中剩余的元素数量
     */
    public final int remaining() {
        return limit - position;
    }

    /**
     * 判断当前位置和限制之间是否存在任何元素。
     *
     * @return  <tt>true</tt> 当且仅当缓冲区中至少还有一个元素剩余
     */
    public final boolean hasRemaining() {
        return position < limit;
    }

    /**
     * 判断此缓冲区是否为只读。
     *
     * @return  当且仅当此缓冲区为只读时返回 <tt>true</tt>
     */
    public abstract boolean isReadOnly();

    /**
     * 告诉此缓冲区是否由可访问的数组支持。
     *
     * <p> 如果此方法返回 <tt>true</tt>，则可以安全地调用 {@link #array()} 和 {@link #arrayOffset()} 方法。
     * </p>
     *
     * @return  <tt>true</tt> 当且仅当此缓冲区由数组支持且不是只读的
     *
     * @since 1.6
     */
    public abstract boolean hasArray();

    /**
     * 返回支持此缓冲区的数组&nbsp;&nbsp;<i>(可选操作)</i>。
     *
     * <p> 此方法旨在使基于数组的缓冲区能够更高效地传递给本地代码。具体子类为此方法提供了更具类型安全性的返回值。
     *
     * <p> 对此缓冲区内容的修改将导致返回数组的内容被修改，反之亦然。
     *
     * <p> 在调用此方法之前，请调用 {@link #hasArray hasArray} 方法以确保此缓冲区具有可访问的支持数组。  </p>
     *
     * @return  支持此缓冲区的数组
     *
     * @throws  ReadOnlyBufferException
     *          如果此缓冲区由数组支持但为只读
     *
     * @throws  UnsupportedOperationException
     *          如果此缓冲区没有可访问的支持数组
     *
     * @since 1.6
     */
    public abstract Object array();

    /**
     * 返回此缓冲区后备数组中第一个元素的偏移量&nbsp;&nbsp;<i>(可选操作)</i>。
     *
     * <p> 如果此缓冲区由数组支持，则缓冲区位置 <i>p</i>
     * 对应于数组索引 <i>p</i>&nbsp;+&nbsp;<tt>arrayOffset()</tt>。
     *
     * <p> 在调用此方法之前，请先调用 {@link #hasArray hasArray} 方法，
     * 以确保此缓冲区具有可访问的后备数组。  </p>
     *
     * @return  此缓冲区数组中第一个元素的偏移量
     *
     * @throws  ReadOnlyBufferException
     *          如果此缓冲区由数组支持但为只读
     *
     * @throws  UnsupportedOperationException
     *          如果此缓冲区不由可访问的数组支持
     *
     * @since 1.6
     */
    public abstract int arrayOffset();

    /**
     * 判断此缓冲区是否为
     * <a href="ByteBuffer.html#direct"><i>直接</i></a>缓冲区。
     *
     * @return  当且仅当此缓冲区为直接缓冲区时返回 <tt>true</tt>
     *
     * @since 1.6
     */
    public abstract boolean isDirect();


    // -- 包私有方法，用于边界检查等 --

    /**
     * 检查当前位置是否小于限制，如果不小于限制则抛出 {@link
     * BufferUnderflowException}，然后递增位置。
     *
     * @return  递增前的位置值
     */
    final int nextGetIndex() {                          // 包私有
        if (position >= limit)
            throw new BufferUnderflowException();
        return position++;
    }

    final int nextGetIndex(int nb) {                    // 包私有
        if (limit - position < nb)
            throw new BufferUnderflowException();
        int p = position;
        position += nb;
        return p;
    }

    /**
     * 检查当前位置是否小于限制，如果不小于限制则抛出 {@link
     * BufferOverflowException}，然后递增位置。
     *
     * @return  递增前的位置值
     */
    final int nextPutIndex() {                          // 包私有
        if (position >= limit)
            throw new BufferOverflowException();
        return position++;
    }

    final int nextPutIndex(int nb) {                    // 包私有
        if (limit - position < nb)
            throw new BufferOverflowException();
        int p = position;
        position += nb;
        return p;
    }

    /**
     * 检查给定的索引是否超出限制，如果索引不小于限制或小于零，则抛出 {@link
     * IndexOutOfBoundsException}。
     */
    final int checkIndex(int i) {                       // 包私有
        if ((i < 0) || (i >= limit))
            throw new IndexOutOfBoundsException();
        return i;
    }

    final int checkIndex(int i, int nb) {               // 包私有
        if ((i < 0) || (nb > limit - i))
            throw new IndexOutOfBoundsException();
        return i;
    }

    final int markValue() {                             // 包私有
        return mark;
    }

    final void truncate() {                             // 包私有
        mark = -1;
        position = 0;
        limit = 0;
        capacity = 0;
    }

    final void discardMark() {                          // 包私有
        mark = -1;
    }

    static void checkBounds(int off, int len, int size) { // 包私有
        if ((off | len | (off + len) | (size - (off + len))) < 0)
            throw new IndexOutOfBoundsException();
    }

}

package org.top.java.netty.source.util.internal.shaded.org.jctools.queues;

/**
 * This interface is provided for monitoring purposes only and is only available on queues where it is easy to
 * provide it. The producer/consumer progress indicators usually correspond with the number of elements
 * offered/polled, but they are not guaranteed to maintain that semantic.
 */

/**
 * 该接口仅用于监控目的，且仅在易于提供的队列上可用。生产者/消费者进度指示器通常与已提供/已轮询的元素数量对应，
 * 但不能保证其始终维持这种语义。
 */
public interface QueueProgressIndicators
{

    /**
     * This method has no concurrent visibility semantics. The value returned may be negative. Under normal
     * circumstances 2 consecutive calls to this method can offer an idea of progress made by producer threads
     * by subtracting the 2 results though in extreme cases (if producers have progressed by more than 2^64)
     * this may also fail.<br/>
     * This value will normally indicate number of elements passed into the queue, but may under some
     * circumstances be a derivative of that figure. This method should not be used to derive size or
     * emptiness.
     *
     * @return the current value of the producer progress index
     */

    /**
     * 该方法没有并发可见性语义。返回的值可能为负数。在正常情况下，连续两次调用此方法可以通过减去两个结果来了解生产者线程的进度，
     * 但在极端情况下（如果生产者的进度超过2^64），这也可能失败。<br/>
     * 该值通常表示已传递到队列中的元素数量，但在某些情况下可能是该值的衍生值。此方法不应用于推导大小或判断是否为空。
     *
     * @return 生产者进度索引的当前值
     */
    long currentProducerIndex();

    /**
     * This method has no concurrent visibility semantics. The value returned may be negative. Under normal
     * circumstances 2 consecutive calls to this method can offer an idea of progress made by consumer threads
     * by subtracting the 2 results though in extreme cases (if consumers have progressed by more than 2^64)
     * this may also fail.<br/>
     * This value will normally indicate number of elements taken out of the queue, but may under some
     * circumstances be a derivative of that figure. This method should not be used to derive size or
     * emptiness.
     *
     * @return the current value of the consumer progress index
     */

    /**
     * 该方法没有并发可见性语义。返回的值可能为负数。在正常情况下，通过连续两次调用此方法并减去两次结果，
     * 可以了解消费者线程的进展情况，但在极端情况下（如果消费者的进度超过了2^64），这也可能失败。<br/>
     * 该值通常表示从队列中取出的元素数量，但在某些情况下可能是该数字的派生值。此方法不应用于推导大小或
     * 是否为空。
     *
     * @return 消费者进度索引的当前值
     */
    long currentConsumerIndex();
}

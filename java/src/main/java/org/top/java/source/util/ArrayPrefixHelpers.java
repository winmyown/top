/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
package org.top.java.source.util;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * ForkJoin tasks to perform Arrays.parallelPrefix operations.
 *
 * @author Doug Lea
 * @since 1.8
 */
class ArrayPrefixHelpers {
    private ArrayPrefixHelpers() {}; // non-instantiable

    /*
     * Parallel prefix (aka cumulate, scan) task classes
     * are based loosely on Guy Blelloch's original
     * algorithm (http://www.cs.cmu.edu/~scandal/alg/scan.html):
     *  Keep dividing by two to threshold segment size, and then:
     *   Pass 1: Create tree of partial sums for each segment
     *   Pass 2: For each segment, cumulate with offset of left sibling
     *
     * This version improves performance within FJ framework mainly by
     * allowing the second pass of ready left-hand sides to proceed
     * even if some right-hand side first passes are still executing.
     * It also combines first and second pass for leftmost segment,
     * and skips the first pass for rightmost segment (whose result is
     * not needed for second pass).  It similarly manages to avoid
     * requiring that users supply an identity basis for accumulations
     * by tracking those segments/subtasks for which the first
     * existing element is used as base.
     *
     * Managing this relies on ORing some bits in the pendingCount for
     * phases/states: CUMULATE, SUMMED, and FINISHED. CUMULATE is the
     * main phase bit. When false, segments compute only their sum.
     * When true, they cumulate array elements. CUMULATE is set at
     * root at beginning of second pass and then propagated down. But
     * it may also be set earlier for subtrees with lo==0 (the left
     * spine of tree). SUMMED is a one bit join count. For leafs, it
     * is set when summed. For internal nodes, it becomes true when
     * one child is summed.  When the second child finishes summing,
     * we then moves up tree to trigger the cumulate phase. FINISHED
     * is also a one bit join count. For leafs, it is set when
     * cumulated. For internal nodes, it becomes true when one child
     * is cumulated.  When the second child finishes cumulating, it
     * then moves up tree, completing at the root.
     *
     * To better exploit locality and reduce overhead, the compute
     * method loops starting with the current task, moving if possible
     * to one of its subtasks rather than forking.
     *
     * As usual for this sort of utility, there are 4 versions, that
     * are simple copy/paste/adapt variants of each other.  (The
     * double and int versions differ from long version soley by
     * replacing "long" (with case-matching)).
     */

    // see above
    static final int CUMULATE = 1;
    static final int SUMMED   = 2;
    static final int FINISHED = 4;

    /** The smallest subtask array partition size to use as threshold */
    static final int MIN_PARTITION = 16;

    static final class CumulateTask<T> extends CountedCompleter<Void> {
        final T[] array;
        final BinaryOperator<T> function;
        CumulateTask<T> left, right;
        T in, out;
        final int lo, hi, origin, fence, threshold;

        /** Root task constructor */
        public CumulateTask(CumulateTask<T> parent,
                            BinaryOperator<T> function,
                            T[] array, int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.lo = this.origin = lo; this.hi = this.fence = hi;
            int p;
            this.threshold =
                    (p = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3))
                    <= MIN_PARTITION ? MIN_PARTITION : p;
        }

        /** Subtask constructor */
        CumulateTask(CumulateTask<T> parent, BinaryOperator<T> function,
                     T[] array, int origin, int fence, int threshold,
                     int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.origin = origin; this.fence = fence;
            this.threshold = threshold;
            this.lo = lo; this.hi = hi;
        }

        @SuppressWarnings("unchecked")
        public final void compute() {
            final BinaryOperator<T> fn;
            final T[] a;
            if ((fn = this.function) == null || (a = this.array) == null)
                throw new NullPointerException();    // hoist checks
            int th = threshold, org = origin, fnc = fence, l, h;
            CumulateTask<T> t = this;
            outer: while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
                if (h - l > th) {
                    CumulateTask<T> lt = t.left, rt = t.right, f;
                    if (lt == null) {                // first pass
                        int mid = (l + h) >>> 1;
                        f = rt = t.right =
                                new CumulateTask<T>(t, fn, a, org, fnc, th, mid, h);
                        t = lt = t.left  =
                                new CumulateTask<T>(t, fn, a, org, fnc, th, l, mid);
                    }
                    else {                           // possibly refork
                        T pin = t.in;
                        lt.in = pin;
                        f = t = null;
                        if (rt != null) {
                            T lout = lt.out;
                            rt.in = (l == org ? lout :
                                     fn.apply(pin, lout));
                            for (int c;;) {
                                if (((c = rt.getPendingCount()) & CUMULATE) != 0)
                                    break;
                                if (rt.compareAndSetPendingCount(c, c|CUMULATE)){
                                    t = rt;
                                    break;
                                }
                            }
                        }
                        for (int c;;) {
                            if (((c = lt.getPendingCount()) & CUMULATE) != 0)
                                break;
                            if (lt.compareAndSetPendingCount(c, c|CUMULATE)) {
                                if (t != null)
                                    f = t;
                                t = lt;
                                break;
                            }
                        }
                        if (t == null)
                            break;
                    }
                    if (f != null)
                        f.fork();
                }
                else {
                    int state; // Transition to sum, cumulate, or both
                    for (int b;;) {
                        if (((b = t.getPendingCount()) & FINISHED) != 0)
                            break outer;                      // already done
                        state = ((b & CUMULATE) != 0? FINISHED :
                                 (l > org) ? SUMMED : (SUMMED|FINISHED));
                        if (t.compareAndSetPendingCount(b, b|state))
                            break;
                    }

                    T sum;
                    if (state != SUMMED) {
                        int first;
                        if (l == org) {                       // leftmost; no in
                            sum = a[org];
                            first = org + 1;
                        }
                        else {
                            sum = t.in;
                            first = l;
                        }
                        for (int i = first; i < h; ++i)       // cumulate
                            a[i] = sum = fn.apply(sum, a[i]);
                    }
                    else if (h < fnc) {                       // skip rightmost
                        sum = a[l];
                        for (int i = l + 1; i < h; ++i)       // sum only
                            sum = fn.apply(sum, a[i]);
                    }
                    else
                        sum = t.in;
                    t.out = sum;
                    for (CumulateTask<T> par;;) {             // propagate
                        if ((par = (CumulateTask<T>)t.getCompleter()) == null) {
                            if ((state & FINISHED) != 0)      // enable join
                                t.quietlyComplete();
                            break outer;
                        }
                        int b = par.getPendingCount();
                        if ((b & state & FINISHED) != 0)
                            t = par;                          // both done
                        else if ((b & state & SUMMED) != 0) { // both summed
                            int nextState; CumulateTask<T> lt, rt;
                            if ((lt = par.left) != null &&
                                (rt = par.right) != null) {
                                T lout = lt.out;
                                par.out = (rt.hi == fnc ? lout :
                                           fn.apply(lout, rt.out));
                            }
                            int refork = (((b & CUMULATE) == 0 &&
                                           par.lo == org) ? CUMULATE : 0);
                            if ((nextState = b|state|refork) == b ||
                                par.compareAndSetPendingCount(b, nextState)) {
                                state = SUMMED;               // drop finished
                                t = par;
                                if (refork != 0)
                                    par.fork();
                            }
                        }
                        else if (par.compareAndSetPendingCount(b, b|state))
                            break outer;                      // sib not ready
                    }
                }
            }
        }
    }

    static final class LongCumulateTask extends CountedCompleter<Void> {
        final long[] array;
        final LongBinaryOperator function;
        LongCumulateTask left, right;
        long in, out;
        final int lo, hi, origin, fence, threshold;

        /** Root task constructor */
        public LongCumulateTask(LongCumulateTask parent,
                                LongBinaryOperator function,
                                long[] array, int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.lo = this.origin = lo; this.hi = this.fence = hi;
            int p;
            this.threshold =
                    (p = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3))
                    <= MIN_PARTITION ? MIN_PARTITION : p;
        }

        /** Subtask constructor */
        LongCumulateTask(LongCumulateTask parent, LongBinaryOperator function,
                         long[] array, int origin, int fence, int threshold,
                         int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.origin = origin; this.fence = fence;
            this.threshold = threshold;
            this.lo = lo; this.hi = hi;
        }

        public final void compute() {
            final LongBinaryOperator fn;
            final long[] a;
            if ((fn = this.function) == null || (a = this.array) == null)
                throw new NullPointerException();    // hoist checks
            int th = threshold, org = origin, fnc = fence, l, h;
            LongCumulateTask t = this;
            outer: while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
                if (h - l > th) {
                    LongCumulateTask lt = t.left, rt = t.right, f;
                    if (lt == null) {                // first pass
                        int mid = (l + h) >>> 1;
                        f = rt = t.right =
                                new LongCumulateTask(t, fn, a, org, fnc, th, mid, h);
                        t = lt = t.left  =
                                new LongCumulateTask(t, fn, a, org, fnc, th, l, mid);
                    }
                    else {                           // possibly refork
                        long pin = t.in;
                        lt.in = pin;
                        f = t = null;
                        if (rt != null) {
                            long lout = lt.out;
                            rt.in = (l == org ? lout :
                                     fn.applyAsLong(pin, lout));
                            for (int c;;) {
                                if (((c = rt.getPendingCount()) & CUMULATE) != 0)
                                    break;
                                if (rt.compareAndSetPendingCount(c, c|CUMULATE)){
                                    t = rt;
                                    break;
                                }
                            }
                        }
                        for (int c;;) {
                            if (((c = lt.getPendingCount()) & CUMULATE) != 0)
                                break;
                            if (lt.compareAndSetPendingCount(c, c|CUMULATE)) {
                                if (t != null)
                                    f = t;
                                t = lt;
                                break;
                            }
                        }
                        if (t == null)
                            break;
                    }
                    if (f != null)
                        f.fork();
                }
                else {
                    int state; // Transition to sum, cumulate, or both
                    for (int b;;) {
                        if (((b = t.getPendingCount()) & FINISHED) != 0)
                            break outer;                      // already done
                        state = ((b & CUMULATE) != 0? FINISHED :
                                 (l > org) ? SUMMED : (SUMMED|FINISHED));
                        if (t.compareAndSetPendingCount(b, b|state))
                            break;
                    }

                    long sum;
                    if (state != SUMMED) {
                        int first;
                        if (l == org) {                       // leftmost; no in
                            sum = a[org];
                            first = org + 1;
                        }
                        else {
                            sum = t.in;
                            first = l;
                        }
                        for (int i = first; i < h; ++i)       // cumulate
                            a[i] = sum = fn.applyAsLong(sum, a[i]);
                    }
                    else if (h < fnc) {                       // skip rightmost
                        sum = a[l];
                        for (int i = l + 1; i < h; ++i)       // sum only
                            sum = fn.applyAsLong(sum, a[i]);
                    }
                    else
                        sum = t.in;
                    t.out = sum;
                    for (LongCumulateTask par;;) {            // propagate
                        if ((par = (LongCumulateTask)t.getCompleter()) == null) {
                            if ((state & FINISHED) != 0)      // enable join
                                t.quietlyComplete();
                            break outer;
                        }
                        int b = par.getPendingCount();
                        if ((b & state & FINISHED) != 0)
                            t = par;                          // both done
                        else if ((b & state & SUMMED) != 0) { // both summed
                            int nextState; LongCumulateTask lt, rt;
                            if ((lt = par.left) != null &&
                                (rt = par.right) != null) {
                                long lout = lt.out;
                                par.out = (rt.hi == fnc ? lout :
                                           fn.applyAsLong(lout, rt.out));
                            }
                            int refork = (((b & CUMULATE) == 0 &&
                                           par.lo == org) ? CUMULATE : 0);
                            if ((nextState = b|state|refork) == b ||
                                par.compareAndSetPendingCount(b, nextState)) {
                                state = SUMMED;               // drop finished
                                t = par;
                                if (refork != 0)
                                    par.fork();
                            }
                        }
                        else if (par.compareAndSetPendingCount(b, b|state))
                            break outer;                      // sib not ready
                    }
                }
            }
        }
    }

    static final class DoubleCumulateTask extends CountedCompleter<Void> {
        final double[] array;
        final DoubleBinaryOperator function;
        DoubleCumulateTask left, right;
        double in, out;
        final int lo, hi, origin, fence, threshold;

        /** Root task constructor */
        public DoubleCumulateTask(DoubleCumulateTask parent,
                                  DoubleBinaryOperator function,
                                  double[] array, int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.lo = this.origin = lo; this.hi = this.fence = hi;
            int p;
            this.threshold =
                    (p = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3))
                    <= MIN_PARTITION ? MIN_PARTITION : p;
        }

        /** Subtask constructor */
        DoubleCumulateTask(DoubleCumulateTask parent, DoubleBinaryOperator function,
                           double[] array, int origin, int fence, int threshold,
                           int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.origin = origin; this.fence = fence;
            this.threshold = threshold;
            this.lo = lo; this.hi = hi;
        }

        public final void compute() {
            final DoubleBinaryOperator fn;
            final double[] a;
            if ((fn = this.function) == null || (a = this.array) == null)
                throw new NullPointerException();    // hoist checks
            int th = threshold, org = origin, fnc = fence, l, h;
            DoubleCumulateTask t = this;
            outer: while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
                if (h - l > th) {
                    DoubleCumulateTask lt = t.left, rt = t.right, f;
                    if (lt == null) {                // first pass
                        int mid = (l + h) >>> 1;
                        f = rt = t.right =
                                new DoubleCumulateTask(t, fn, a, org, fnc, th, mid, h);
                        t = lt = t.left  =
                                new DoubleCumulateTask(t, fn, a, org, fnc, th, l, mid);
                    }
                    else {                           // possibly refork
                        double pin = t.in;
                        lt.in = pin;
                        f = t = null;
                        if (rt != null) {
                            double lout = lt.out;
                            rt.in = (l == org ? lout :
                                     fn.applyAsDouble(pin, lout));
                            for (int c;;) {
                                if (((c = rt.getPendingCount()) & CUMULATE) != 0)
                                    break;
                                if (rt.compareAndSetPendingCount(c, c|CUMULATE)){
                                    t = rt;
                                    break;
                                }
                            }
                        }
                        for (int c;;) {
                            if (((c = lt.getPendingCount()) & CUMULATE) != 0)
                                break;
                            if (lt.compareAndSetPendingCount(c, c|CUMULATE)) {
                                if (t != null)
                                    f = t;
                                t = lt;
                                break;
                            }
                        }
                        if (t == null)
                            break;
                    }
                    if (f != null)
                        f.fork();
                }
                else {
                    int state; // Transition to sum, cumulate, or both
                    for (int b;;) {
                        if (((b = t.getPendingCount()) & FINISHED) != 0)
                            break outer;                      // already done
                        state = ((b & CUMULATE) != 0? FINISHED :
                                 (l > org) ? SUMMED : (SUMMED|FINISHED));
                        if (t.compareAndSetPendingCount(b, b|state))
                            break;
                    }

                    double sum;
                    if (state != SUMMED) {
                        int first;
                        if (l == org) {                       // leftmost; no in
                            sum = a[org];
                            first = org + 1;
                        }
                        else {
                            sum = t.in;
                            first = l;
                        }
                        for (int i = first; i < h; ++i)       // cumulate
                            a[i] = sum = fn.applyAsDouble(sum, a[i]);
                    }
                    else if (h < fnc) {                       // skip rightmost
                        sum = a[l];
                        for (int i = l + 1; i < h; ++i)       // sum only
                            sum = fn.applyAsDouble(sum, a[i]);
                    }
                    else
                        sum = t.in;
                    t.out = sum;
                    for (DoubleCumulateTask par;;) {            // propagate
                        if ((par = (DoubleCumulateTask)t.getCompleter()) == null) {
                            if ((state & FINISHED) != 0)      // enable join
                                t.quietlyComplete();
                            break outer;
                        }
                        int b = par.getPendingCount();
                        if ((b & state & FINISHED) != 0)
                            t = par;                          // both done
                        else if ((b & state & SUMMED) != 0) { // both summed
                            int nextState; DoubleCumulateTask lt, rt;
                            if ((lt = par.left) != null &&
                                (rt = par.right) != null) {
                                double lout = lt.out;
                                par.out = (rt.hi == fnc ? lout :
                                           fn.applyAsDouble(lout, rt.out));
                            }
                            int refork = (((b & CUMULATE) == 0 &&
                                           par.lo == org) ? CUMULATE : 0);
                            if ((nextState = b|state|refork) == b ||
                                par.compareAndSetPendingCount(b, nextState)) {
                                state = SUMMED;               // drop finished
                                t = par;
                                if (refork != 0)
                                    par.fork();
                            }
                        }
                        else if (par.compareAndSetPendingCount(b, b|state))
                            break outer;                      // sib not ready
                    }
                }
            }
        }
    }

    static final class IntCumulateTask extends CountedCompleter<Void> {
        final int[] array;
        final IntBinaryOperator function;
        IntCumulateTask left, right;
        int in, out;
        final int lo, hi, origin, fence, threshold;

        /** Root task constructor */
        public IntCumulateTask(IntCumulateTask parent,
                               IntBinaryOperator function,
                               int[] array, int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.lo = this.origin = lo; this.hi = this.fence = hi;
            int p;
            this.threshold =
                    (p = (hi - lo) / (ForkJoinPool.getCommonPoolParallelism() << 3))
                    <= MIN_PARTITION ? MIN_PARTITION : p;
        }

        /** Subtask constructor */
        IntCumulateTask(IntCumulateTask parent, IntBinaryOperator function,
                        int[] array, int origin, int fence, int threshold,
                        int lo, int hi) {
            super(parent);
            this.function = function; this.array = array;
            this.origin = origin; this.fence = fence;
            this.threshold = threshold;
            this.lo = lo; this.hi = hi;
        }

        public final void compute() {
            final IntBinaryOperator fn;
            final int[] a;
            if ((fn = this.function) == null || (a = this.array) == null)
                throw new NullPointerException();    // hoist checks
            int th = threshold, org = origin, fnc = fence, l, h;
            IntCumulateTask t = this;
            outer: while ((l = t.lo) >= 0 && (h = t.hi) <= a.length) {
                if (h - l > th) {
                    IntCumulateTask lt = t.left, rt = t.right, f;
                    if (lt == null) {                // first pass
                        int mid = (l + h) >>> 1;
                        f = rt = t.right =
                                new IntCumulateTask(t, fn, a, org, fnc, th, mid, h);
                        t = lt = t.left  =
                                new IntCumulateTask(t, fn, a, org, fnc, th, l, mid);
                    }
                    else {                           // possibly refork
                        int pin = t.in;
                        lt.in = pin;
                        f = t = null;
                        if (rt != null) {
                            int lout = lt.out;
                            rt.in = (l == org ? lout :
                                     fn.applyAsInt(pin, lout));
                            for (int c;;) {
                                if (((c = rt.getPendingCount()) & CUMULATE) != 0)
                                    break;
                                if (rt.compareAndSetPendingCount(c, c|CUMULATE)){
                                    t = rt;
                                    break;
                                }
                            }
                        }
                        for (int c;;) {
                            if (((c = lt.getPendingCount()) & CUMULATE) != 0)
                                break;
                            if (lt.compareAndSetPendingCount(c, c|CUMULATE)) {
                                if (t != null)
                                    f = t;
                                t = lt;
                                break;
                            }
                        }
                        if (t == null)
                            break;
                    }
                    if (f != null)
                        f.fork();
                }
                else {
                    int state; // Transition to sum, cumulate, or both
                    for (int b;;) {
                        if (((b = t.getPendingCount()) & FINISHED) != 0)
                            break outer;                      // already done
                        state = ((b & CUMULATE) != 0? FINISHED :
                                 (l > org) ? SUMMED : (SUMMED|FINISHED));
                        if (t.compareAndSetPendingCount(b, b|state))
                            break;
                    }

                    int sum;
                    if (state != SUMMED) {
                        int first;
                        if (l == org) {                       // leftmost; no in
                            sum = a[org];
                            first = org + 1;
                        }
                        else {
                            sum = t.in;
                            first = l;
                        }
                        for (int i = first; i < h; ++i)       // cumulate
                            a[i] = sum = fn.applyAsInt(sum, a[i]);
                    }
                    else if (h < fnc) {                       // skip rightmost
                        sum = a[l];
                        for (int i = l + 1; i < h; ++i)       // sum only
                            sum = fn.applyAsInt(sum, a[i]);
                    }
                    else
                        sum = t.in;
                    t.out = sum;
                    for (IntCumulateTask par;;) {            // propagate
                        if ((par = (IntCumulateTask)t.getCompleter()) == null) {
                            if ((state & FINISHED) != 0)      // enable join
                                t.quietlyComplete();
                            break outer;
                        }
                        int b = par.getPendingCount();
                        if ((b & state & FINISHED) != 0)
                            t = par;                          // both done
                        else if ((b & state & SUMMED) != 0) { // both summed
                            int nextState; IntCumulateTask lt, rt;
                            if ((lt = par.left) != null &&
                                (rt = par.right) != null) {
                                int lout = lt.out;
                                par.out = (rt.hi == fnc ? lout :
                                           fn.applyAsInt(lout, rt.out));
                            }
                            int refork = (((b & CUMULATE) == 0 &&
                                           par.lo == org) ? CUMULATE : 0);
                            if ((nextState = b|state|refork) == b ||
                                par.compareAndSetPendingCount(b, nextState)) {
                                state = SUMMED;               // drop finished
                                t = par;
                                if (refork != 0)
                                    par.fork();
                            }
                        }
                        else if (par.compareAndSetPendingCount(b, b|state))
                            break outer;                      // sib not ready
                    }
                }
            }
        }
    }
}

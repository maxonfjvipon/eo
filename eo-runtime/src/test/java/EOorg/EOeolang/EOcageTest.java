/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2024 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * @checkstyle PackageNameCheck (10 lines)
 */
package EOorg.EOeolang;

import java.util.concurrent.atomic.AtomicReference;
import org.eolang.AtFree;
import org.eolang.Atom;
import org.eolang.Data;
import org.eolang.Dataized;
import org.eolang.ExAbstract;
import org.eolang.PhCopy;
import org.eolang.PhDefault;
import org.eolang.PhMethod;
import org.eolang.PhTracedEnclosure;
import org.eolang.PhWith;
import org.eolang.Phi;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test case for {@link EOcage}.
 * @since 0.19
 */
final class EOcageTest {

    @Test
    void writesAndReads() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(cage, new Data.ToPhi(1L));
        MatcherAssert.assertThat(
            new Dataized(cage).take(Long.class),
            Matchers.equalTo(1L)
        );
    }

    @Test
    void checksThatEmptyCageHasIdentity() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        MatcherAssert.assertThat(
            new Dataized(cage.attr("ν").get()).take(Long.class),
            Matchers.greaterThan(0L)
        );
    }

    @Test
    void writesItselfToItself() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(
            cage,
            new PhWith(
                new EOcage(Phi.Φ).copy(), 0, new Data.ToPhi(1L)
            )
        );
        final Phi first = cage.copy();
        EOcageTest.writeTo(cage, first);
        final Phi second = cage.copy();
        EOcageTest.writeTo(cage, second);
        MatcherAssert.assertThat(
            new Dataized(cage).take(Long.class),
            Matchers.equalTo(1L)
        );
    }

    // [] > test
    //   cage 0 > c
    //   [x] > dummy
    //     x > @
    //   seq > @
    //     *
    //       c.write (dummy 1)
    //       c.write (dummy c')
    //       c.x.x.eq 1
    @Test
    void writesDummyToItself() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        new Dataized(
            new PhWith(
                cage.attr("write").get().copy(),
                0, new PhWith(new EOcageTest.Dummy(Phi.Φ), 0, new Data.ToPhi(1L))
            )
        ).take();
        new Dataized(
            new PhWith(
                cage.attr("write").get().copy(),
                0, new PhWith(new EOcageTest.Dummy(Phi.Φ), 0, cage.copy())
            )
        ).take();
        MatcherAssert.assertThat(
            new Dataized(
                cage.attr("x").get().attr("x").get()
            ).take(Long.class),
            Matchers.equalTo(1L)
        );
    }

    @Test
    void overwritesCagedObject() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(
            cage,
            new PhWith(
                new EOcageTest.Dummy(Phi.Φ),
                0, new Data.ToPhi(1L)
            )
        );
        MatcherAssert.assertThat(
            new Dataized(new PhMethod(cage, "x")).take(Long.class),
            Matchers.equalTo(1L)
        );
        EOcageTest.writeTo(
            cage,
            new PhWith(
                new EOcageTest.Dummy(Phi.Φ),
                0, new Data.ToPhi(2L)
            )
        );
        MatcherAssert.assertThat(
            new Dataized(new PhMethod(cage, "x")).take(Long.class),
            Matchers.equalTo(2L)
        );
    }

    @Test
    void makesTrueCopy() {
        final Phi first = new EOcage(Phi.Φ).copy();
        first.attr(0).put(new Data.ToPhi(1L));
        final Phi second = first.copy();
        new Dataized(
            new PhWith(
                second.attr("write").get(),
                "x", new Data.ToPhi(2L)
            )
        ).take();
        MatcherAssert.assertThat(
            new Dataized(first).take(Long.class),
            Matchers.equalTo(1L)
        );
    }

    @Test
    void writesAndRewritesPrimitive() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(cage, new Data.ToPhi(1L));
        MatcherAssert.assertThat(
            new Dataized(cage).take(Long.class),
            Matchers.equalTo(1L)
        );
        EOcageTest.writeTo(cage, new Data.ToPhi(5L));
        MatcherAssert.assertThat(
            new Dataized(cage).take(Long.class),
            Matchers.equalTo(5L)
        );
    }

    @Test
    void doesNotWritePrimitivesFormedDifferently() {
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(cage, new Data.ToPhi(1L));
        Assertions.assertThrows(
            EOerror.ExError.class,
            () -> EOcageTest.writeTo(cage, new Data.ToPhi("Hello world"))
        );
    }

    @Test
    void doesNotWriteBoundedMethod() {
        final Phi five = new Data.ToPhi(5L);
        final Phi ten = new PhWith(
            five.attr("plus").get().copy(),
            "x",
            new Data.ToPhi(5L)
        );
        final Phi cage = new EOcage(Phi.Φ).copy();
        EOcageTest.writeTo(cage, five);
        Assertions.assertThrows(
            EOerror.ExError.class,
            () -> EOcageTest.writeTo(cage, ten)
        );
    }

    @Test
    void writesBoundedCopyOfTheSameBase() {
        final Phi dummy = new Dummy(Phi.Φ);
        Assertions.assertDoesNotThrow(
            () -> EOcageTest.writeTo(
                new PhWith(new EOcage(Phi.Φ).copy(), 0, dummy),
                new PhWith(new PhCopy(dummy), "x", new Data.ToPhi("Hello world"))
            )
        );
    }

    private static void writeTo(final Phi cage, final Phi obj) {
        final Phi write = cage.attr("write").get().copy();
        write.attr(0).put(obj);
        new Dataized(write).take();
    }

    /**
     * Cases to test behaviour of cage with recursion.
     * @since 0.1
     */
    @Nested
    @Execution(ExecutionMode.SAME_THREAD)
    class RecursionTests {

        /**
         * DEPTH.
         */
        private static final int MAX_DEPTH = 50;

        @BeforeEach
        void setDepth() {
            System.setProperty(
                PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME, String.valueOf(MAX_DEPTH)
            );
        }

        @AfterEach
        void clearDepth() {
            System.clearProperty(PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME);
        }

        @Test
        void throwsExceptionIfRecursion() {
            final Phi cage = new EOcage(Phi.Φ).copy();
            writeTo(cage, cage);
            Assertions.assertThrows(
                ExAbstract.class,
                () -> new Dataized(cage).take(),
                "We expect the exception to be thrown since we have recursion here"
            );
        }

        @Test
        void doesNotThrowExceptionIfSmallDepth() {
            final Phi cage = new EOcage(Phi.Φ).copy();
            EOcageTest.writeTo(
                cage,
                new RecursiveDummy(EOcageTest.RecursionTests.MAX_DEPTH / 2, cage)
            );
            Assertions.assertDoesNotThrow(
                () -> new Dataized(cage).take(),
                String.format(
                    "We expect that dataizing of nested cage which recursion depth is less than property %s = %s does not throw %s",
                    PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME,
                    System.getProperty(PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME),
                    ExAbstract.class
                )
            );
        }

        /**
         * The boundary case when the depth is equal to the maximum allowed.
         */
        @Test
        void doesNotThrowExceptionIfMaxDepth() {
            final Phi cage = new EOcage(Phi.Φ).copy();
            writeTo(
                cage,
                new RecursiveDummy(MAX_DEPTH, cage)
            );
            Assertions.assertDoesNotThrow(
                () -> new Dataized(cage).take(),
                String.format(
                    "We expect that dataizing of nested cage which recursion depth is equal to property %s = %s does not throw %s",
                    PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME,
                    System.getProperty(PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME),
                    ExAbstract.class
                )
            );
        }

        @Test
        void throwsExceptionIfBigDepth() {
            final Phi cage = new EOcage(Phi.Φ).copy();
            writeTo(
                cage,
                new RecursiveDummy(EOcageTest.RecursionTests.MAX_DEPTH + 1, cage)
            );
            Assertions.assertThrows(
                ExAbstract.class,
                () -> new Dataized(cage).take(),
                String.format(
                    "We expect that dataizing of nested cage which recursion depth is more than property %s = %s does not throw %s",
                    PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME,
                    System.getProperty(PhTracedEnclosure.MAX_CAGE_RECURSION_DEPTH_PROPERTY_NAME),
                    ExAbstract.class
                )
            );
        }

        /**
         * Recursive {@link Phi}.
         * @since 0.1
         */
        private final class RecursiveDummy extends PhDefault implements Atom {

            /**
             * How many times should we met the cage while dataizing it eventually.
             */
            private final int depth;

            /**
             * The cage.
             */
            private final Phi cage;

            /**
             * Counts how many times we already met the cage.
             */
            private final AtomicReference<Integer> counter;

            /**
             * Ctor.
             * @param depth Depth.
             * @param cage Cage.
             */
            RecursiveDummy(
                final int depth, final Phi cage
            ) {
                this.depth = depth;
                this.cage = cage;
                this.counter = new AtomicReference<>(0);
            }

            @Override
            public Phi lambda() {
                final Phi ret;
                this.counter.getAndUpdate(val -> val + 1);
                if (this.counter.get() == this.depth) {
                    ret = new Data.ToPhi(0L);
                } else {
                    ret = this.cage;
                }
                return ret;
            }
        }
    }

    /**
     * Dummy Phi.
     * @since 1.0
     */
    public static final class Dummy extends PhDefault {
        /**
         * Ctor.
         * @param sigma Sigma
         */
        Dummy(final Phi sigma) {
            super(sigma);
            this.add("x", new AtFree());
        }
    }
}

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
package EOorg.EOeolang.EOio;

import EOorg.EOeolang.EOseq;
import EOorg.EOeolang.EOtuple$EOempty;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.eolang.AtComposite;
import org.eolang.Data;
import org.eolang.Dataized;
import org.eolang.PhCopy;
import org.eolang.PhDefault;
import org.eolang.PhMethod;
import org.eolang.PhWith;
import org.eolang.Phi;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link EOstdout}.
 * @since 0.1
 * @todo #2931:30min Enable the test {@link EOstdoutTest#doesNotPrintTwiceOnFloatComparisonMethods}.
 *  The test was disabled after new rho logic was introduced and {@link org.eolang.PhConst} stopped
 *  working properly. Need to enable the test when it's possible.
 */
public final class EOstdoutTest {
    @Test
    public void printsFromTuple() {
        final Phi tuple = Phi.Φ.attr("org").get()
            .attr("eolang").get()
            .attr("tuple").get();
        final Phi copy = tuple.copy();
        copy.attr(0).put(tuple.attr("empty").get());
        copy.attr(1).put(new Data.ToPhi("Hello"));
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Phi ret = copy.attr("at").get().copy();
        ret.attr(0).put(new Data.ToPhi(0L));
        final Phi stdout = new EOstdout(Phi.Φ, new PrintStream(stream)).copy();
        stdout.attr(0).put(ret);
        new Dataized(stdout).take(Boolean.class);
        MatcherAssert.assertThat(
            stream.toString(),
            Matchers.equalTo("Hello")
        );
    }

    @Test
    public void printsString() {
        final Phi format = new Data.ToPhi("Hello, world!\n");
        final Phi phi = new PhWith(
            new PhCopy(new EOstdout(Phi.Φ)),
            "text",
            format
        );
        MatcherAssert.assertThat(
            new Dataized(phi).take(Boolean.class),
            Matchers.equalTo(true)
        );
    }

    @ParameterizedTest
    @CsvSource({"lt", "gt", "lte", "gte", "eq"})
    public void doesNotPrintTwiceOnIntComparisonMethods(final String method) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final String str = "Hello world";
        new Dataized(
            new PrintWithCmp(
                new PhMethod(
                    new Data.ToPhi(1L),
                    method
                ),
                new Data.ToPhi(2L),
                new PhWith(
                    new EOstdout(Phi.Φ, new PrintStream(stream)),
                    "text",
                    new Data.ToPhi(str)
                )
            )
        ).data();
        MatcherAssert.assertThat(
            stream.toString(),
            Matchers.equalTo(str)
        );
    }

    @ParameterizedTest()
    @CsvSource({"lt", "gt", "lte", "gte"})
    @Disabled
    public void doesNotPrintTwiceOnFloatComparisonMethods(final String method) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final String str = "Hello world";
        new Dataized(
            new PrintWithCmp(
                new PhMethod(
                    new Data.ToPhi(1.0),
                    method
                ),
                new Data.ToPhi(3.0),
                new PhWith(
                    new EOstdout(Phi.Φ, new PrintStream(stream)),
                    "text",
                    new Data.ToPhi(str)
                )
            )
        ).data();
        MatcherAssert.assertThat(
            stream.toString(),
            Matchers.equalTo(str)
        );
    }

    /**
     * PrintWithCmp Phi.
     *
     * @since 1.0
     */
    private static class PrintWithCmp extends PhDefault {
        /**
         * Ctor.
         *
         * @param method Comparison PhMethod ("lt", "gt", "lte", "gte")
         * @param value Phi value to be compared
         * @param stdout Phi object with printing a string via {@link EOstdout} object
         */
        PrintWithCmp(final Phi method, final Phi value, final Phi stdout) {
            super(Phi.Φ);
            this.add(
                "φ",
                new AtComposite(
                    this,
                    self -> new Data.ToPhi(
                        new Dataized(
                            new PhWith(
                                method,
                                0,
                                new PhWith(
                                    new EOseq(Phi.Φ),
                                    0,
                                    new PhWith(
                                        new PhWith(
                                            new EOtuple$EOempty(Phi.Φ)
                                                .attr("with")
                                                .get()
                                                .copy(),
                                            0,
                                            stdout
                                        ).attr("with").get().copy(),
                                        0, value
                                    )
                                )
                            )
                        ).data()
                    )
                )
            );
        }
    }
}

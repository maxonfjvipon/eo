/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Chainable}.
 * @since 0.1
 */
final class ChainableTest {

    @Test
    void permitsChainOnIdentifier() {
        MatcherAssert.assertThat(
            "identifier heads are the canonical chainable shape — `foo.bar` is valid",
            new Chainable(new Value(Value.Kind.IDENTIFIER, "foo", 0, 3)).accepted(),
            Matchers.is(true)
        );
    }

    @Test
    void permitsChainOnInteger() {
        MatcherAssert.assertThat(
            "integer heads admit a method chain per §3.6 — `42.as-bytes` is valid",
            new Chainable(new Value(Value.Kind.INTEGER, "42", 0, 2)).accepted(),
            Matchers.is(true)
        );
    }

    @Test
    void permitsChainOnParenGroup() {
        MatcherAssert.assertThat(
            "paren-group heads admit a method chain — `(a.b).c` is valid",
            new Chainable(new Value(Value.Kind.GROUP, "(a)", 0, 3)).accepted(),
            Matchers.is(true)
        );
    }

    @Test
    void rejectsChainOnStar() {
        MatcherAssert.assertThat(
            "the `*` star tuple in argument position cannot carry a chain — `f *.with` is invalid",
            new Chainable(new Value(Value.Kind.STAR, "*", 0, 1)).accepted(),
            Matchers.is(false)
        );
    }

    @Test
    void rejectsChainOnBytes() {
        MatcherAssert.assertThat(
            "bytes literals don't chain in argument position; the line-head case is separate",
            new Chainable(new Value(Value.Kind.BYTES, "01-", 0, 3)).accepted(),
            Matchers.is(false)
        );
    }

    @Test
    void rejectsChainOnHex() {
        MatcherAssert.assertThat(
            "hex literals don't chain in argument position; the line-head case is separate",
            new Chainable(new Value(Value.Kind.HEX, "0xFF", 0, 4)).accepted(),
            Matchers.is(false)
        );
    }
}

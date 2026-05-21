/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * Whether a {@link Value} head may carry a {@code .method} chain
 * after it.
 *
 * <p>Per §3.6, application heads include identifiers, root tokens,
 * paren groups, and data literals; all of these admit a method chain.
 * The {@code *} star tuple, bytes, and hex literals do not chain in
 * argument position (chains on them are valid only as the line's
 * head — see {@link LnApplication}). This predicate is consulted by
 * the argument reader to decide whether to attempt a
 * {@code tokens.readChain()} after an argument value.</p>
 *
 * @since 0.1
 * @checkstyle BooleanExpressionComplexityCheck (50 lines)
 */
final class Chainable {

    /**
     * The value whose chainability is in question.
     */
    private final Value head;

    /**
     * Ctor.
     * @param value The head value
     */
    Chainable(final Value value) {
        this.head = value;
    }

    /**
     * Whether the head may carry a {@code .method} chain after it.
     * @return True when the value's kind is one that admits a chain
     *  in argument position
     */
    boolean accepted() {
        return this.head.kind() == Value.Kind.IDENTIFIER
            || this.head.kind() == Value.Kind.ROOT
            || this.head.kind() == Value.Kind.GROUP
            || this.head.kind() == Value.Kind.INTEGER
            || this.head.kind() == Value.Kind.FLOAT
            || this.head.kind() == Value.Kind.STRING;
    }
}

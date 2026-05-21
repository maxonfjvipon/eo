/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * The "reversed-dispatch receiver carries no binding" check — R-6.6.3
 * of the spec.
 *
 * <p>The first argument of a horizontal reversed-dispatch line is the
 * receiver of the chain; it must not itself carry an inline binding.
 * This object encapsulates that validation as a single
 * {@link #check()} call.</p>
 *
 * @since 0.1
 */
final class ReceiverBinding {

    /**
     * The receiver value to validate.
     */
    private final Value receiver;

    /**
     * The line span (for error positioning).
     */
    private final Span span;

    /**
     * Ctor.
     * @param value The receiver value
     * @param src The line span
     */
    ReceiverBinding(final Value value, final Span src) {
        this.receiver = value;
        this.span = src;
    }

    /**
     * Throw {@link ParseError} when the receiver carries a binding;
     * do nothing otherwise.
     */
    void check() {
        if (this.receiver.binding() != null) {
            throw new ParseError(
                this.span.line(), this.receiver.pos(),
                "reversed-dispatch receiver cannot carry a binding"
            );
        }
    }
}

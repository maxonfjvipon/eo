/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import java.util.List;

/**
 * The all-or-nothing binding check across an argument group — R-6.6.2
 * of the spec.
 *
 * <p>Inside a same-indent application argument list, every argument
 * must either carry an inline binding ({@code :label} or {@code :N})
 * or none. Mixing bound and unbound arguments is a parse error
 * positioned at the first divergent argument. An empty list or a
 * single argument is always valid.</p>
 *
 * @since 0.1
 */
final class AllOrNothing {

    /**
     * The argument group to validate.
     */
    private final List<Value> args;

    /**
     * The line span (for error positioning).
     */
    private final Span span;

    /**
     * Ctor.
     * @param values The argument group
     * @param src The line span
     */
    AllOrNothing(final List<Value> values, final Span src) {
        this.args = values;
        this.span = src;
    }

    /**
     * Throw {@link ParseError} when the group mixes bound and unbound
     * arguments; do nothing otherwise.
     */
    void check() {
        if (this.args.size() >= 2) {
            final boolean head = this.args.get(0).binding() != null;
            for (int idx = 1; idx < this.args.size(); idx = idx + 1) {
                final boolean bound = this.args.get(idx).binding() != null;
                if (bound != head) {
                    throw new ParseError(
                        this.span.line(), this.args.get(idx).pos(),
                        "argument bindings must be all-or-nothing"
                    );
                }
            }
        }
    }
}

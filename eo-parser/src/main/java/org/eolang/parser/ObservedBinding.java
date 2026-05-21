/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * The cross-line binding observation for a freshly-pushed child —
 * R-6.6.2 / R-6.6.3 / R-3.12.3 of the spec.
 *
 * <p>When a new child line is pushed onto the indent stack, its outer
 * binding (if any) must be checked against its parent context:</p>
 *
 * <ul>
 *   <li>arg-bearing parents ({@link Kind#HEAD}, {@link Kind#HMETHOD},
 *   {@link Kind#VAPPLICATION}) participate in the all-or-nothing
 *   group across deeper children;</li>
 *   <li>{@link Kind#BARE_REVERSED} parents: the first child is the
 *   receiver (no binding allowed); subsequent children participate
 *   in the all-or-nothing group;</li>
 *   <li>formation / top-level parents reject any binding (R-3.12.3).</li>
 * </ul>
 *
 * @since 0.1
 */
final class ObservedBinding {

    /**
     * The indent stack (the new child is already on top).
     */
    private final Stack stack;

    /**
     * The child's outer binding, or {@code null} when absent.
     */
    private final String outer;

    /**
     * The child line's source span.
     */
    private final Span span;

    /**
     * Ctor.
     * @param stk The indent stack
     * @param label Outer binding label or {@code null}
     * @param src The child line's span
     */
    ObservedBinding(final Stack stk, final String label, final Span src) {
        this.stack = stk;
        this.outer = label;
        this.span = src;
    }

    /**
     * Validate the child's binding against its parent context and
     * record the observation on the parent level. Throws a
     * {@link ParseError} on mismatch.
     */
    void observe() {
        final Level parent = this.stack.below();
        if (parent == null || parent.kind() == Kind.BARE_FORMATION) {
            this.rejectBinding();
        } else if (parent.kind() == Kind.BARE_REVERSED) {
            this.observeReversedChild(parent);
        } else if (parent.kind() == Kind.HEAD
            || parent.kind() == Kind.HMETHOD
            || parent.kind() == Kind.VAPPLICATION) {
            parent.observeBinding(this.outer != null, this.span);
        }
    }

    /**
     * Reject a binding under a formation body or at top-level
     * (R-3.12.3).
     */
    private void rejectBinding() {
        if (this.outer != null) {
            throw new ParseError(
                this.span.line(), this.span.indent(),
                "binding allowed only in argument position"
            );
        }
    }

    /**
     * Handle a child under a {@link Kind#BARE_REVERSED} parent. The
     * first child is the receiver and must not carry a binding
     * (R-6.6.3); subsequent children participate in the
     * all-or-nothing group (R-6.6.2).
     * @param parent The bare-reversed parent
     */
    private void observeReversedChild(final Level parent) {
        if (parent.children() > 1) {
            parent.observeBinding(this.outer != null, this.span);
        } else if (this.outer != null) {
            throw new ParseError(
                this.span.line(), this.span.indent(),
                "reversed-dispatch receiver cannot carry a binding"
            );
        }
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * Blank-line bookkeeping for a single {@link Line} — §6.5 of the
 * spec.
 *
 * <p>R-6.5.3 caps consecutive blanks at one (enforced in
 * {@link LnBlank}). R-6.5.4 forbids a blank line before a plain child
 * or between two plain siblings — enforced here by
 * {@link #checkPlain()}.</p>
 *
 * <p>R-6.5.5 requires exactly one blank line between the meta header
 * and whatever follows; enforced by {@link #enterAfterMeta()}, which
 * fires from the first non-meta non-blank line when the parser has
 * accumulated meta directives but not yet seen any blank.</p>
 *
 * @since 0.1
 */
final class Blanks {

    /**
     * The line being checked.
     */
    private final Span span;

    /**
     * The global parser state (blank counter, meta-header window).
     */
    private final Globals globals;

    /**
     * The directives sink, used to emit recoverable errors.
     */
    private final Emit emit;

    /**
     * Ctor.
     * @param src The line's source span
     * @param state The global parser state
     * @param sink The directives sink
     */
    Blanks(final Span src, final Globals state, final Emit sink) {
        this.span = src;
        this.globals = state;
        this.emit = sink;
    }

    /**
     * Report a blank line in front of a plain child or between two
     * plain siblings — illegal per R-6.5.4. Master children
     * (formations, atoms, only-phi formations, {@code +>} tests)
     * are exempt and call this method only when they want to *not*
     * exempt themselves.
     */
    void checkPlain() {
        this.enterAfterMeta();
        if (this.globals.pendingBlanks() > 0) {
            this.emit.error(
                this.span.line(), this.span.indent(),
                "blank line before a plain object is forbidden (R-6.5.4); only master objects (formations, atoms, only-phi formations, +> tests) may be preceded by a blank line"
            );
        }
    }

    /**
     * Report R-6.5.5 — the first non-meta non-blank line after the
     * meta header must be preceded by exactly one blank line. Closes
     * the meta-header window so subsequent lines are not re-checked.
     */
    void enterAfterMeta() {
        if (this.globals.inMetaHeader()) {
            if (this.globals.pendingBlanks() == 0) {
                this.emit.error(
                    this.span.line(), this.span.indent(),
                    "missing blank line between meta header and the first non-meta line (R-6.5.5); exactly one blank must separate them"
                );
            } else {
                this.globals.clearBlanks();
            }
            this.globals.closeMetaHeader();
        }
    }
}

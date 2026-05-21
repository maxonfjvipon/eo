/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import java.util.List;

/**
 * Pending comment block waiting to attach to the next named object —
 * §6.4 of the spec.
 *
 * <p>A comment block accumulates in {@link Globals#pendingComments()}
 * as each {@link LnComment} arrives. The next named line — any
 * formation, application, method or reversed line whose suffix carries
 * a name — instantiates this class and calls {@link #attach()} to
 * either flush the block into {@code /object/comments/comment} (when
 * the line is named, sits at the same indent, and has no intervening
 * blank line — R-6.5.2) or leave it as dangling per R-6.4.2.</p>
 *
 * @since 0.1
 */
final class Comments {

    /**
     * Global parser state holding the pending comment block.
     */
    private final Globals globals;

    /**
     * XMIR emitter for the flushed {@code <comment>} elements.
     */
    private final Emit emit;

    /**
     * Source span of the upcoming line.
     */
    private final Span span;

    /**
     * Whether the upcoming line carries a name suffix.
     */
    private final boolean named;

    /**
     * Ctor.
     * @param state Global parser state
     * @param sink XMIR emitter
     * @param src Source span of the upcoming line
     * @param has True when the upcoming line carries a name suffix
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    Comments(final Globals state, final Emit sink, final Span src, final boolean has) {
        this.globals = state;
        this.emit = sink;
        this.span = src;
        this.named = has;
    }

    /**
     * Flush the pending comment block when the upcoming line is named,
     * sits at the same indent as the block head, and is not separated
     * by a blank line. Otherwise leave the buffer untouched (a dangling
     * block is reported by the EOF check).
     */
    void attach() {
        final List<Span> pending = this.globals.pendingComments();
        if (pending.isEmpty()) {
            return;
        }
        final Span head = pending.get(0);
        if (this.named
            && head.indent() == this.span.indent()
            && this.globals.pendingBlanks() == 0) {
            this.emit.comment(pending, this.span.line());
            this.globals.clearComments();
        }
    }
}

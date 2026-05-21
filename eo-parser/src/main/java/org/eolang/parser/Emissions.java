/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Value}-to-XMIR renderer, scoped to a single {@link Emit}
 * sink — §9.4 of the spec.
 *
 * <p>An {@code Emissions} instance owns one {@link Emit} and exposes
 * the §9.0.3 / §9.4 / §9.4.2 emission recipes as instance methods:
 * {@link #openValue} for a head value, {@link #emitArg} for an
 * argument, {@link #expression} for a full application expression
 * read from a {@link Tokens} stream, and {@link #bytesCarrier} for
 * the inner {@code <o base='Φ.bytes'>HEX&lt;/o>} carrier under
 * numeric, hex, and string literals. The pure data transforms — the
 * binding-tag mapping, escape decoding, and chainable-head predicate —
 * live in their own classes ({@link BindingTag},
 * {@link UnescapedBody}, {@link Chainable}).</p>
 *
 * @since 0.1
 * @checkstyle CyclomaticComplexityCheck (400 lines)
 * @checkstyle BooleanExpressionComplexityCheck (400 lines)
 */
@SuppressWarnings({"PMD.UnnecessaryLocalRule", "PMD.CognitiveComplexity"})
final class Emissions {

    /**
     * The directives sink to write into.
     */
    private final Emit emit;

    /**
     * Ctor.
     * @param sink The directives sink
     */
    Emissions(final Emit sink) {
        this.emit = sink;
    }

    /**
     * Emit a full application expression read from {@code tokens} —
     * head, optional {@code .method} chain, and optional horizontal
     * args (§9.0.3). The outermost {@code <o>} (head or chain's last
     * link) is left <em>open</em> for the caller to close.
     * @param name Name to attach to the outermost {@code <o>}, or
     *  {@code null}
     * @param tokens Token reader (cursor positioned at the head)
     * @param line Source line number
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    void expression(final String name, final Tokens tokens, final int line) {
        final Value head = tokens.readValue();
        if (Emissions.reversedDispatch(tokens, head)) {
            tokens.seek(tokens.cursor() + 1);
            final List<Value> rargs = tokens.readArgs();
            this.emit.object(name, ".".concat(head.raw()), line, head.pos());
            for (final Value arg : rargs) {
                this.emitArg(arg, line);
            }
            return;
        }
        final List<MethodChain> chain = tokens.readChain();
        final List<Value> args = tokens.readArgs();
        if (chain.isEmpty()) {
            this.openValue(name, head, line);
        } else {
            this.openValue(null, head, line);
            this.emit.close();
            for (int idx = 0; idx < chain.size() - 1; idx = idx + 1) {
                final MethodChain link = chain.get(idx);
                this.emit.object(null, ".".concat(link.name()), line, link.dot());
                this.emit.method();
                this.emit.close();
            }
            final MethodChain last = chain.get(chain.size() - 1);
            this.emit.object(name, ".".concat(last.name()), line, last.dot());
            this.emit.method();
        }
        for (final Value arg : args) {
            this.emitArg(arg, line);
        }
    }

    /**
     * Open an {@code <o>} for a value as a head element. The element
     * remains open after this call so chain links or horizontal args
     * can be added inside it (or, for nested expressions, so the
     * caller can close it).
     * @param name Name attribute (or {@code null})
     * @param value The value
     * @param line Source line
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    void openValue(final String name, final Value value, final int line) {
        if (value.kind() == Value.Kind.INTEGER || value.kind() == Value.Kind.FLOAT) {
            this.emit.object(name, "Φ.number", line, value.pos());
            this.bytesCarrier(
                line, value.pos(), new Hex(Double.parseDouble(value.raw())).asString()
            );
        } else if (value.kind() == Value.Kind.HEX) {
            this.emit.object(name, "Φ.number", line, value.pos());
            this.bytesCarrier(
                line, value.pos(),
                new Hex((double) Long.parseLong(value.raw().substring(2), 16)).asString()
            );
        } else if (value.kind() == Value.Kind.BYTES) {
            this.emit.object(name, "Φ.bytes", line, value.pos());
            this.emit.object(null, null, line, value.pos());
            this.emit.set(value.raw());
            this.emit.close();
        } else if (value.kind() == Value.Kind.STRING) {
            this.emit.object(name, "Φ.string", line, value.pos());
            this.bytesCarrier(
                line, value.pos(),
                new Hex(
                    new UnescapedBody(
                        value.raw().substring(1, value.raw().length() - 1)
                    ).decoded()
                ).asString()
            );
        } else if (value.kind() == Value.Kind.STAR) {
            this.emit.object(name, "Φ.tuple", line, value.pos());
            this.emit.star();
        } else if (value.kind() == Value.Kind.ROOT) {
            this.emit.object(name, Emissions.rootBase(value.raw()), line, value.pos());
        } else if (value.kind() == Value.Kind.GROUP) {
            final String inner = value.raw().substring(1, value.raw().length() - 1);
            final int phi = Emissions.topLevelInlinePhi(inner);
            if (phi >= 0) {
                this.inlinePhi(name, inner, phi, value.pos() + 1, line);
            } else {
                final Span sub = new Span(
                    " ".repeat(value.pos() + 1).concat(inner), line
                );
                this.expression(name, new Tokens(sub.body(), sub), line);
            }
        } else {
            this.emit.object(name, value.raw(), line, value.pos());
        }
    }

    /**
     * Emit a value as a self-contained argument child — opened and
     * immediately closed. If the value carries an inline binding
     * (§3.12), attaches {@code @as}.
     * @param value The value
     * @param line Source line
     */
    void emitArg(final Value value, final int line) {
        final List<MethodChain> tail = value.chain();
        if (tail.isEmpty()) {
            this.openValue(null, value, line);
            if (value.binding() != null) {
                this.emit.slot(new BindingTag(value.binding()).encoded());
            }
            this.emit.close();
        } else {
            this.openValue(null, value, line);
            this.emit.close();
            for (int idx = 0; idx < tail.size() - 1; idx = idx + 1) {
                final MethodChain link = tail.get(idx);
                this.emit.object(null, ".".concat(link.name()), line, link.dot());
                this.emit.method();
                this.emit.close();
            }
            final MethodChain last = tail.get(tail.size() - 1);
            this.emit.object(null, ".".concat(last.name()), line, last.dot());
            this.emit.method();
            if (value.binding() != null) {
                this.emit.slot(new BindingTag(value.binding()).encoded());
            }
            this.emit.close();
        }
    }

    /**
     * Emit the inner {@code <o base='Φ.bytes'><o>HEX&lt;/o>&lt;/o>}
     * data carrier used by numeric, hex and string literals.
     * @param line Source line
     * @param pos Source column
     * @param hex Pre-formatted hex string (BB-BB-... or empty form)
     */
    void bytesCarrier(final int line, final int pos, final String hex) {
        this.emit.object(null, "Φ.bytes", line, pos);
        this.emit.object(null, null, line, pos);
        this.emit.set(hex);
        this.emit.close();
        this.emit.close();
    }

    /**
     * Map a source root character to its XMIR symbol per §9.3.
     * @param raw Source character (one of {@code Q}, {@code @},
     *  {@code ^}, {@code $})
     * @return XMIR symbol
     */
    private static String rootBase(final String raw) {
        final String mapped;
        if ("Q".equals(raw)) {
            mapped = "Φ";
        } else if ("@".equals(raw)) {
            mapped = "φ";
        } else if ("^".equals(raw)) {
            mapped = "ρ";
        } else if ("$".equals(raw)) {
            mapped = "ξ";
        } else {
            mapped = raw;
        }
        return mapped;
    }

    /**
     * Whether the cursor is at a reversed-dispatch separator — a
     * {@code .} immediately followed by a space or end-of-body. Used
     * to recognise inner reversed-dispatch expressions inside paren
     * groups (e.g. {@code (mod. y 4)}), where the {@code name.} form
     * opens a new dispatch chain.
     * @param tokens Token reader (positioned after the head)
     * @param head Just-read head value
     * @return True when the cursor is at a reversed-dispatch dot
     */
    private static boolean reversedDispatch(final Tokens tokens, final Value head) {
        final boolean reversed;
        if (head.kind() == Value.Kind.IDENTIFIER
            && !tokens.atEnd() && tokens.current() == '.') {
            final int probe = tokens.cursor() + 1;
            reversed = probe >= tokens.body().length()
                || tokens.body().charAt(probe) == ' ';
        } else {
            reversed = false;
        }
        return reversed;
    }

    /**
     * Find the position of a top-level {@code > [} inline-phi marker
     * in {@code body} (depth-zero, not inside strings). Returns the
     * index of the {@code >} char, or {@code -1} if none.
     * @param body The inner body of a paren group
     * @return Index of {@code >} char, or {@code -1}
     */
    private static int topLevelInlinePhi(final String body) {
        int depth = 0;
        boolean instr = false;
        int found = -1;
        int idx = 0;
        while (idx < body.length() - 2 && found < 0) {
            final char glyph = body.charAt(idx);
            if (instr) {
                if (glyph == '\\' && idx + 1 < body.length()) {
                    idx = idx + 1;
                } else if (glyph == '"') {
                    instr = false;
                }
            } else if (glyph == '"') {
                instr = true;
            } else if (glyph == '(') {
                depth = depth + 1;
            } else if (glyph == ')') {
                depth = depth - 1;
            } else if (depth == 0 && glyph == '>'
                && body.charAt(idx + 1) == ' ' && body.charAt(idx + 2) == '[') {
                found = idx;
            }
            idx = idx + 1;
        }
        return found;
    }

    /**
     * Emit an inline-phi formation (§3.10) detected inside a paren
     * group. The formation is anonymous (no {@code > name} suffix);
     * its {@code φ} slot holds the body expression.
     * @param name Name to attach to the formation's {@code <o>}
     * @param inner The full inner body of the paren group
     * @param phi Index of the {@code >} that begins {@code > [}
     * @param column Absolute source column of the first body char
     * @param line Source line
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    private void inlinePhi(
        final String name, final String inner,
        final int phi, final int column, final int line
    ) {
        final int bracket = phi + 2;
        final int close = inner.indexOf(']', bracket);
        final String lhs = inner.substring(0, phi).stripTrailing();
        final String params = inner.substring(bracket + 1, close);
        this.emit.object(name, null, line, column);
        int pcol = column + bracket + 1;
        for (final String param : Emissions.splitParams(params)) {
            final String mapped;
            if (param.equals("@")) {
                mapped = "φ";
            } else {
                mapped = param;
            }
            this.emit.voidParam(mapped, line, pcol);
            pcol = pcol + param.length() + 1;
        }
        final Span sub = new Span(" ".repeat(column).concat(lhs), line);
        this.expression("φ", new Tokens(sub.body(), sub), line);
        this.emit.close();
    }

    /**
     * Split a parameter-list body into individual names by single
     * spaces.
     * @param text Param text (without surrounding brackets)
     * @return Names in source order
     */
    private static List<String> splitParams(final String text) {
        final List<String> out = new ArrayList<>(0);
        int idx = 0;
        while (idx < text.length()) {
            int end = idx;
            while (end < text.length() && text.charAt(end) != ' ') {
                end = end + 1;
            }
            out.add(text.substring(idx, end));
            if (end < text.length()) {
                idx = end + 1;
            } else {
                idx = end;
            }
        }
        return out;
    }
}

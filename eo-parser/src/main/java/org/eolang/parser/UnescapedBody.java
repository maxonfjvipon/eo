/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * A raw string body with its escape sequences decoded — §9.7.3 / §9.7.4
 * of the spec.
 *
 * <p>Supports the single-character escapes ({@code \n}, {@code \t},
 * {@code \r}, {@code \b}, {@code \f}, {@code \"}, {@code \'},
 * {@code \\}), the octal escape {@code \NNN}, and the
 * {@code \\uXXXX} unicode escape (legacy {@code \\uu...uXXXX} form is
 * accepted and collapsed to the canonical single-{@code u} form).
 * Unknown backslash sequences pass through verbatim.</p>
 *
 * @since 0.1
 */
final class UnescapedBody {

    /**
     * The raw body without surrounding quotes.
     */
    private final String inner;

    /**
     * Ctor.
     * @param raw Source body (no quotes)
     */
    UnescapedBody(final String raw) {
        this.inner = raw;
    }

    /**
     * Decode the escape sequences in the body and return the canonical
     * text.
     * @return Decoded body — escape sequences replaced by the
     *  corresponding code points
     */
    String decoded() {
        final StringBuilder out = new StringBuilder(this.inner.length());
        int idx = 0;
        while (idx < this.inner.length()) {
            final char glyph = this.inner.charAt(idx);
            if (glyph != '\\' || idx + 1 >= this.inner.length()) {
                out.append(glyph);
                idx = idx + 1;
                continue;
            }
            final char next = this.inner.charAt(idx + 1);
            if (next == 'u') {
                idx = UnescapedBody.appendUnicode(out, this.inner, idx + 1);
            } else if (next >= '0' && next <= '7') {
                idx = UnescapedBody.appendOctal(out, this.inner, idx + 1);
            } else {
                out.append(UnescapedBody.singleChar(glyph, next));
                idx = idx + 2;
            }
        }
        return out.toString();
    }

    /**
     * Decode {@code \NNN} octal (1-3 digits) into a single character.
     * @param out Output buffer
     * @param body String body
     * @param start First octal digit position
     * @return Index past the consumed digits
     */
    private static int appendOctal(
        final StringBuilder out, final String body, final int start
    ) {
        int cursor = start;
        int value = 0;
        while (cursor < body.length()
            && cursor < start + 3
            && body.charAt(cursor) >= '0' && body.charAt(cursor) <= '7') {
            value = value * 8 + body.charAt(cursor) - '0';
            cursor = cursor + 1;
        }
        out.append((char) value);
        return cursor;
    }

    /**
     * Decode {@code \\uXXXX} unicode (legacy {@code \\uu...uXXXX} form
     * collapsed to one {@code u}) into a single character.
     * @param out Output buffer
     * @param body String body
     * @param start First {@code u} position
     * @return Index past the consumed escape
     */
    private static int appendUnicode(
        final StringBuilder out, final String body, final int start
    ) {
        int cursor = start;
        while (cursor < body.length() && body.charAt(cursor) == 'u') {
            cursor = cursor + 1;
        }
        if (cursor + 4 > body.length()) {
            out.append('\\').append(body, start, body.length());
        } else {
            out.append(
                (char) Integer.parseInt(body.substring(cursor, cursor + 4), 16)
            );
        }
        return cursor + 4;
    }

    /**
     * Decode a single-character escape sequence.
     * @param head The backslash
     * @param next The character after the backslash
     * @return Decoded character(s) — unknown sequences pass through
     *  verbatim
     */
    private static String singleChar(final char head, final char next) {
        final String decoded;
        if (next == 'n') {
            decoded = String.valueOf((char) 10);
        } else if (next == 't') {
            decoded = String.valueOf((char) 9);
        } else if (next == 'r') {
            decoded = String.valueOf((char) 13);
        } else if (next == 'b') {
            decoded = String.valueOf((char) 8);
        } else if (next == 'f') {
            decoded = String.valueOf((char) 12);
        } else if (next == '"' || next == '\'' || next == '\\') {
            decoded = String.valueOf(next);
        } else {
            decoded = new String(new char[]{head, next});
        }
        return decoded;
    }
}

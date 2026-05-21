/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

/**
 * A binding tag — the {@code @as} attribute value emitted for an
 * inline binding (§3.12 / §9.4 of the spec).
 *
 * <p>Numeric labels (e.g. {@code :3}) decode to {@code αN}; identifier
 * labels (e.g. {@code :head}) are emitted verbatim.</p>
 *
 * @since 0.1
 */
final class BindingTag {

    /**
     * The raw binding label as it appears in source after {@code :}.
     */
    private final String raw;

    /**
     * Ctor.
     * @param label The raw label
     */
    BindingTag(final String label) {
        this.raw = label;
    }

    /**
     * Render the label as the {@code @as} attribute value.
     * @return Encoded tag — {@code αN} for numeric labels, verbatim
     *  for identifier labels
     */
    String encoded() {
        final String tag;
        if (!this.raw.isEmpty() && this.raw.chars().allMatch(c -> c >= '0' && c <= '9')) {
            tag = "α".concat(this.raw);
        } else {
            tag = this.raw;
        }
        return tag;
    }
}

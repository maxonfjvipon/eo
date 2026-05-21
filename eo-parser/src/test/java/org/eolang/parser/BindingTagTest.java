/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link BindingTag}.
 * @since 0.1
 */
final class BindingTagTest {

    @Test
    void encodesNumericLabelAsAlpha() {
        MatcherAssert.assertThat(
            "a numeric binding label like '3' must be rendered as 'α3' (alpha + N)",
            new BindingTag("3").encoded(),
            Matchers.equalTo("α3")
        );
    }

    @Test
    void encodesMultiDigitNumericLabelAsAlpha() {
        MatcherAssert.assertThat(
            "every multi-digit numeric label must be prefixed with α, not just single digits",
            new BindingTag("42").encoded(),
            Matchers.equalTo("α42")
        );
    }

    @Test
    void encodesIdentifierLabelVerbatim() {
        MatcherAssert.assertThat(
            "an identifier label must be emitted as-is so XMIR consumers can use the name",
            new BindingTag("head").encoded(),
            Matchers.equalTo("head")
        );
    }

    @Test
    void encodesMixedLabelAsIdentifier() {
        MatcherAssert.assertThat(
            "a label containing any non-digit must fall through to the identifier path",
            new BindingTag("idx1").encoded(),
            Matchers.equalTo("idx1")
        );
    }

    @Test
    void encodesEmptyLabelAsEmpty() {
        MatcherAssert.assertThat(
            "an empty label must round-trip without prefixing — there's nothing to prefix",
            new BindingTag("").encoded(),
            Matchers.equalTo("")
        );
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link UnescapedBody}.
 * @since 0.1
 */
final class UnescapedBodyTest {

    @Test
    void decodesSingleCharNewlineEscape() {
        MatcherAssert.assertThat(
            "the literal sequence \\n must decode to U+000A (newline) per R-9.7.3",
            new UnescapedBody("a\\nb").decoded(),
            Matchers.equalTo("a".concat(String.valueOf((char) 10)).concat("b"))
        );
    }

    @Test
    void decodesOctalEscape() {
        MatcherAssert.assertThat(
            "the octal escape \\012 must decode to U+000A (newline) per R-9.7.3",
            new UnescapedBody("\\012").decoded(),
            Matchers.equalTo(String.valueOf((char) 10))
        );
    }

    @Test
    void decodesUnicodeEscape() {
        MatcherAssert.assertThat(
            "the unicode escape \\u0424 must decode to U+0424 (cyrillic Ef) per R-9.7.3",
            new UnescapedBody("\\u0424").decoded(),
            Matchers.equalTo("Ф")
        );
    }

    @Test
    void preservesUnknownBackslashSequenceVerbatim() {
        MatcherAssert.assertThat(
            "an unknown backslash sequence like \\q must pass through unchanged",
            new UnescapedBody("\\q").decoded(),
            Matchers.equalTo("\\q")
        );
    }

    @Test
    void preservesTextWithoutEscapes() {
        MatcherAssert.assertThat(
            "a body with no backslash must round-trip unchanged through the decoder",
            new UnescapedBody("hello world").decoded(),
            Matchers.equalTo("hello world")
        );
    }
}

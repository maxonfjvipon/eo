/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import com.jcabi.matchers.XhtmlMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * Test case for {@link Blanks}.
 * @since 0.1
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
final class BlanksTest {

    @Test
    void reportsBlankBeforePlainObject() {
        final Globals globals = new Globals();
        globals.blank();
        final Emit emit = new Emit();
        new Blanks(new Span("foo", 2), globals, emit).checkPlain();
        MatcherAssert.assertThat(
            "a blank line before a plain object must produce an /object/errors/error per R-6.5.4",
            BlanksTest.render(emit),
            XhtmlMatchers.hasXPath("/object/errors/error[contains(text(),'plain')]")
        );
    }

    @Test
    void acceptsPlainObjectWithoutPrecedingBlank() {
        final Emit emit = new Emit();
        new Blanks(new Span("foo", 2), new Globals(), emit).checkPlain();
        MatcherAssert.assertThat(
            "a plain object preceded by no blank line must not produce an error",
            BlanksTest.render(emit),
            Matchers.not(XhtmlMatchers.hasXPath("/object/errors/*"))
        );
    }

    @Test
    void reportsMissingBlankAfterMetaHeader() {
        final Globals globals = new Globals();
        globals.markMeta();
        final Emit emit = new Emit();
        new Blanks(new Span("foo", 2), globals, emit).enterAfterMeta();
        MatcherAssert.assertThat(
            "the first non-meta line without a preceding blank must produce an /object/errors/error per R-6.5.5",
            BlanksTest.render(emit),
            XhtmlMatchers.hasXPath("/object/errors/error[contains(text(),'meta header')]")
        );
    }

    @Test
    void closesMetaHeaderWindowWhenBlankPrecedes() {
        final Globals globals = new Globals();
        globals.markMeta();
        globals.blank();
        new Blanks(new Span("foo", 3), globals, new Emit()).enterAfterMeta();
        MatcherAssert.assertThat(
            "after the post-meta blank is consumed the parser must leave the meta-header window",
            globals.inMetaHeader(),
            Matchers.is(false)
        );
    }

    /**
     * Render the emit's directives under a fresh {@code <object/>}.
     * @param emit The emit
     * @return XMIR document
     */
    private static String render(final Emit emit) {
        return new Xembler(
            new Directives().add("object").append(emit.directives())
        ).xmlQuietly();
    }
}

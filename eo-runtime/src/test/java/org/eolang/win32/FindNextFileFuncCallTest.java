/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.win32;

import org.eolang.Data;
import org.eolang.ExFailure;
import org.eolang.Phi;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link FindNextFileFuncCall}.
 * @since 0.76.0
 */
final class FindNextFileFuncCallTest {

    @Test
    void refusesAHandleNobodyOpened() {
        MatcherAssert.assertThat(
            "a number naming no open search must be refused before it reaches the kernel as a handle",
            Assertions.assertThrows(
                ExFailure.class,
                () -> new FindNextFileFuncCall(Phi.Φ.take("win32").copy()).make(
                    new Data.ToPhi(-42)
                ),
                "reading a search that was never started was expected to fail with ExFailure"
            ).getMessage(),
            Matchers.containsString("'search' argument of FindNextFile")
        );
    }
}

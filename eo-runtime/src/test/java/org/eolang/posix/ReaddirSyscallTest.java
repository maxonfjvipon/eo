/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.posix;

import org.eolang.Data;
import org.eolang.ExFailure;
import org.eolang.Phi;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ReaddirSyscall}.
 * @since 0.76.0
 */
final class ReaddirSyscallTest {

    @Test
    void refusesAHandleNobodyOpened() {
        MatcherAssert.assertThat(
            "a number naming no open stream must be refused before it reaches libc as an address",
            Assertions.assertThrows(
                ExFailure.class,
                () -> new ReaddirSyscall(Phi.Φ.take("posix").copy()).make(
                    new Data.ToPhi(-42)
                ),
                "reading a stream that was never opened was expected to fail with ExFailure"
            ).getMessage(),
            Matchers.containsString("'dirp' argument of readdir")
        );
    }
}

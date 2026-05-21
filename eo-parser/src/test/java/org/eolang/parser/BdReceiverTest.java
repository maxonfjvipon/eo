/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link BdReceiver}.
 * @since 0.1
 */
final class BdReceiverTest {

    @Test
    void acceptsReceiverWithoutBinding() {
        Assertions.assertDoesNotThrow(
            () -> new BdReceiver(
                new Value(Value.Kind.IDENTIFIER, "cond", 4, 8),
                new Span("if. cond then else", 1)
            ).check(),
            "a bare receiver is the canonical form for reversed dispatch"
        );
    }

    @Test
    void rejectsReceiverWithBinding() {
        Assertions.assertThrows(
            ParseError.class,
            () -> new BdReceiver(
                new Value(Value.Kind.IDENTIFIER, "cond", 4, 8, "x"),
                new Span("if. cond:x then else", 1)
            ).check(),
            "a receiver carrying a binding must be rejected per R-6.6.3"
        );
    }
}

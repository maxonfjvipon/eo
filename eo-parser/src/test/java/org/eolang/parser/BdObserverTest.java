/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link BdObserver}.
 * @since 0.1
 */
final class BdObserverTest {

    @Test
    void acceptsAbsentBindingAtTopLevel() {
        final Stack stack = new Stack();
        stack.push(0, 1, Kind.HEAD, Openness.OPEN);
        Assertions.assertDoesNotThrow(
            () -> new BdObserver(stack, null, new Span("foo", 1)).observe(),
            "a child without a binding is always legal under any parent"
        );
    }

    @Test
    void rejectsBindingUnderTopLevelOnlyParent() {
        final Stack stack = new Stack();
        stack.push(0, 1, Kind.HEAD, Openness.OPEN);
        Assertions.assertThrows(
            ParseError.class,
            () -> new BdObserver(stack, "tag", new Span("foo", 1)).observe(),
            "a binding on a top-level object is illegal per R-3.12.3"
        );
    }

    @Test
    void rejectsBindingUnderBareFormationParent() {
        final Stack stack = new Stack();
        stack.push(0, 1, Kind.BARE_FORMATION, Openness.OPEN);
        stack.push(2, 2, Kind.HEAD, Openness.OPEN);
        Assertions.assertThrows(
            ParseError.class,
            () -> new BdObserver(stack, "tag", new Span("  foo", 2)).observe(),
            "a binding under a formation body is illegal per R-3.12.3"
        );
    }

    @Test
    void rejectsBindingOnReversedDispatchReceiver() {
        final Stack stack = new Stack();
        stack.push(0, 1, Kind.BARE_REVERSED, Openness.OPEN);
        stack.push(2, 2, Kind.HEAD, Openness.OPEN);
        stack.top().child();
        Assertions.assertThrows(
            ParseError.class,
            () -> new BdObserver(stack, "tag", new Span("  foo", 2)).observe(),
            "a binding on the receiver (first child) of a reversed dispatch is illegal per R-6.6.3"
        );
    }
}

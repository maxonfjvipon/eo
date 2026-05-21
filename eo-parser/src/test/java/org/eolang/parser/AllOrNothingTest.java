/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.parser;

import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link AllOrNothing}.
 * @since 0.1
 */
final class AllOrNothingTest {

    @Test
    void acceptsEmptyArgs() {
        Assertions.assertDoesNotThrow(
            () -> new AllOrNothing(
                Collections.emptyList(), new Span("foo", 1)
            ).check(),
            "an empty arg list must pass the all-or-nothing rule trivially"
        );
    }

    @Test
    void acceptsSingleArg() {
        Assertions.assertDoesNotThrow(
            () -> new AllOrNothing(
                Collections.singletonList(new Value(Value.Kind.IDENTIFIER, "a", 4, 5)),
                new Span("foo a", 1)
            ).check(),
            "a single arg cannot violate the all-or-nothing rule"
        );
    }

    @Test
    void acceptsAllUnboundArgs() {
        Assertions.assertDoesNotThrow(
            () -> new AllOrNothing(
                Arrays.asList(
                    new Value(Value.Kind.IDENTIFIER, "a", 4, 5),
                    new Value(Value.Kind.IDENTIFIER, "b", 6, 7),
                    new Value(Value.Kind.IDENTIFIER, "c", 8, 9)
                ),
                new Span("foo a b c", 1)
            ).check(),
            "all-unbound is a valid uniform mode"
        );
    }

    @Test
    void acceptsAllBoundArgs() {
        Assertions.assertDoesNotThrow(
            () -> new AllOrNothing(
                Arrays.asList(
                    new Value(Value.Kind.IDENTIFIER, "a", 4, 5, "x"),
                    new Value(Value.Kind.IDENTIFIER, "b", 6, 7, "y")
                ),
                new Span("foo a:x b:y", 1)
            ).check(),
            "all-bound is a valid uniform mode"
        );
    }

    @Test
    void rejectsMixedBoundAndUnbound() {
        Assertions.assertThrows(
            ParseError.class,
            () -> new AllOrNothing(
                Arrays.asList(
                    new Value(Value.Kind.IDENTIFIER, "a", 4, 5, "x"),
                    new Value(Value.Kind.IDENTIFIER, "b", 6, 7)
                ),
                new Span("foo a:x b", 1)
            ).check(),
            "a bound arg followed by an unbound one must be rejected per R-6.6.2"
        );
    }

    @Test
    void rejectsUnboundFollowedByBound() {
        Assertions.assertThrows(
            ParseError.class,
            () -> new AllOrNothing(
                Arrays.asList(
                    new Value(Value.Kind.IDENTIFIER, "a", 4, 5),
                    new Value(Value.Kind.IDENTIFIER, "b", 6, 7, "y")
                ),
                new Span("foo a b:y", 1)
            ).check(),
            "an unbound arg followed by a bound one must be rejected per R-6.6.2"
        );
    }

    @Test
    void reportsErrorAtFirstDivergentArg() {
        MatcherAssert.assertThat(
            "the error must point at the column of the first divergent arg",
            Assertions.assertThrows(
                ParseError.class,
                () -> new AllOrNothing(
                    Arrays.asList(
                        new Value(Value.Kind.IDENTIFIER, "a", 4, 5),
                        new Value(Value.Kind.IDENTIFIER, "b", 6, 7),
                        new Value(Value.Kind.IDENTIFIER, "c", 8, 9, "z")
                    ),
                    new Span("foo a b c:z", 1)
                ).check(),
                "the divergent arg's column must be reported"
            ).pos(),
            Matchers.equalTo(8)
        );
    }
}

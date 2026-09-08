/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.inference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pairs, asked for again until asking brings nothing.
 *
 * <p>Answering {@code a.b.c} needs {@code a.b} answered first, and one pass
 * cannot put them in that order, so the passes run until one of them adds
 * nothing. Pairs are only ever added, of which there are finitely many, so it
 * settles.</p>
 *
 * <p>There are two things to ask, and the second is asked only when the first
 * has nothing left to say. A dispatch is answered from what the tables hold
 * and costs a walk of them; what a void holds is answered from the whole of
 * the program's call sites and costs a table built to be read once, so it is
 * worth asking when a pass would otherwise be the last. Every void it names
 * opens the dispatches rooted at that void, and the passes go round again.</p>
 *
 * @since 0.69.0
 * @todo #8274:120min Settle a chain in fewer passes than it has hops.
 *  On eo-runtime this runs 128 passes of {@link Dispatched} and 44 of
 *  {@link Promoted} to settle 41,192 pairs, 77 of the passes adding one pair
 *  apiece, which is 30s of the 40s the tables take to build. The count is the
 *  cost and not the pass: of the 18.7s the dispatches take, {@link Bound} is
 *  9.4s and the rest of what a pass builds 6.5s, while the loop over all
 *  13,325 dispatches is 2.8s and already asks none but the ones left
 *  unanswered. So narrowing that loop further buys 7% and would need an index
 *  of what every answer was read off to be sound, since an answer changes when
 *  the fillings of a void change and not only when the end of its bearer does.
 *  What makes the passes is that a chain is read one hop to a pass, and
 *  bool.eo nests 40 formations that call each other. Read more than one hop, or
 *  ask again only what a new pair reaches, and mind that both change the order
 *  the pairs are learnt in, which {@link Dispatched} is not indifferent to.
 */
final class Settled {

    /**
     * What every dispatch turns out to be.
     */
    private final Dispatched made;

    /**
     * What the voids the program fills one way turn out to be.
     */
    private final Promoted more;

    /**
     * Ctor.
     *
     * @param dispatched What every dispatch turns out to be
     * @param promoted What the voids the program fills one way turn out to be
     */
    Settled(final Dispatched dispatched, final Promoted promoted) {
        this.made = dispatched;
        this.more = promoted;
    }

    /**
     * The pairs, with everything that follows from them added.
     *
     * @param pairs The pairs, each name against the one it is a copy of
     * @return The pairs and the ones worked out from them
     */
    Map<String, String> from(final Map<String, String> pairs) {
        final Map<String, String> found = new LinkedHashMap<>(pairs);
        Map<String, String> answers = this.answers(found);
        while (!answers.isEmpty()) {
            found.putAll(answers);
            answers = this.answers(found);
        }
        return found;
    }

    private Map<String, String> answers(final Map<String, String> pairs) {
        Map<String, String> found = this.made.answers(pairs);
        if (found.isEmpty()) {
            found = this.more.from(pairs);
        }
        return found;
    }
}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2021 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package EOorg.EOeolang;

import org.eolang.AtBound;
import org.eolang.AtFree;
import org.eolang.AtLambda;
import org.eolang.Dataized;
import org.eolang.PhDefault;
import org.eolang.Phi;

/**
 * IF.
 *
 * @since 1.0
 */
public class EObool$EOif extends PhDefault {

    public EObool$EOif(final Phi parent, final EObool up) {
        super(parent);
        this.add("t", new AtFree());
        this.add("f", new AtFree());
        this.add("φ", new AtBound(new AtLambda(this, self -> {
            final Boolean term = new Dataized(
                self.attr("ρ").get()
            ).take(Boolean.class);
            final Phi out;
            if (term) {
                out = self.attr("t").get();
            } else {
                out = self.attr("f").get();
            }
            return out;
        })));
    }

}

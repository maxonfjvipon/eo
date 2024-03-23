/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2024 Objectionary.com
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
package org.eolang.parser;

import com.jcabi.manifests.Manifests;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Stack;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.cactoos.list.ListOf;
import org.eolang.parser.xmir.XmirInfo;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * The PHI-CALCULUS grammar listener for ANTLR4 walker.
 *
 * @since 0.34.0
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 * @checkstyle MethodCountCheck (1300 lines)
 * @checkstyle NestedIfDepthCheck (1300 lines)
 */
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessivePublicCount",
    "PMD.ExcessiveClassLength"
})
public final class XePhiListener implements PhiListener, Iterable<Directive> {
    /**
     * Info about xmir.
     */
    private static final XmirInfo INFO = new XmirInfo();

    /**
     * Package lambda.
     */
    private static final String LAMBDA_PACKAGE = "Package";

    /**
     * Xembly directives we are building (mutable).
     */
    private final Directives dirs;

    /**
     * Attributes stack.
     */
    private final Stack<String> attributes;

    /**
     * XMIR properties.
     */
    private final Stack<String> properties;

    /**
     * Objects.
     */
    private final Deque<Objects> objs;

    /**
     * Packages.
     */
    private final Collection<String> packages;

    /**
     * When we start.
     */
    private final long start;

    /**
     * The name of it.
     */
    private final String name;

    /**
     * Ctor.
     * @param nme The name of it
     */
    public XePhiListener(final String nme) {
        this.name = nme;
        this.dirs = new Directives();
        this.objs = new ArrayDeque<>();
        this.attributes = new Stack<>();
        this.properties = new Stack<>();
        this.packages = new ListOf<>();
        this.start = System.nanoTime();
    }

    @Override
    public void enterProgram(final PhiParser.ProgramContext ctx) {
        this.objs.add(new Objects.ObjXembly());
        this.dirs.add("program")
            .attr("name", this.name)
            .attr("version", Manifests.read("EO-Version"))
            .attr("revision", Manifests.read("EO-Revision"))
            .attr("dob", Manifests.read("EO-Dob"))
            .attr(
                "time",
                ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            )
            .comment(XePhiListener.INFO)
            .add("listing").set(new SourceText(ctx)).up()
            .add("errors").up()
            .add("sheets").up()
            .add("license").up()
            .add("metas").up();
        if (ctx.object() == null || ctx.object().formation() == null) {
            this.objects().start();
        }
    }

    @Override
    public void exitProgram(final PhiParser.ProgramContext ctx) {
        if (!this.packages.isEmpty()) {
            final String pckg = String.join(".", this.packages);
            this.dirs.xpath("metas[last()]").strict(1)
                .add("meta")
                .add("head").set("package").up()
                .add("tail").set(pckg).up()
                .add("part").set(pckg).up()
                .up().up();
        }
        if (ctx.object() == null || ctx.object().formation() == null) {
            this.objects().leave();
        }
        this.dirs.add("objects")
            .append(this.objs.pollLast())
            .up()
            .attr("ms", (System.nanoTime() - this.start) / (1000L * 1000L));
    }

    @Override
    public void enterObject(final PhiParser.ObjectContext ctx) {
        // Nothing here
    }

    @Override
    public void exitObject(final PhiParser.ObjectContext ctx) {
        // Nothing here
    }

    @Override
    public void enterFormation(final PhiParser.FormationContext ctx) {
        this.properties.push("name");
    }

    @Override
    public void exitFormation(final PhiParser.FormationContext ctx) {
        this.properties.pop();
        if (!this.properties.empty() && !XePhiListener.hasLambdaPackage(ctx.bindings())) {
            this.objects().prop("abstract").leave();
        }
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void enterScoped(final PhiParser.ScopedContext ctx) {
        if (ctx.HOME() != null) {
            this.objects().prop("base", "Q").leave();
        } else if (ctx.XI() != null) {
            this.objects().prop("base", "$").leave();
        }
    }

    @Override
    public void exitScoped(final PhiParser.ScopedContext ctx) {
        // Nothing here
    }

    @Override
    public void enterBindings(final PhiParser.BindingsContext ctx) {
        if (XePhiListener.hasLambdaPackage(ctx)) {
            this.packages.add(this.attributes.peek());
            this.objs.add(new Objects.ObjXembly());
        }
    }

    @Override
    public void exitBindings(final PhiParser.BindingsContext ctx) {
        if (XePhiListener.hasLambdaPackage(ctx)) {
            this.objs.poll();
        }
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void enterBinding(final PhiParser.BindingContext ctx) {
        if (ctx.alphaBinding() != null) {
            if (ctx.alphaBinding().attribute().VERTEX() != null) {
                this.objs.add(new Objects.ObjXembly());
            }
            this.objects().start();
        }
        if (ctx.emptyBinding() != null) {
            if (ctx.emptyBinding().attribute().VERTEX() != null) {
                this.objs.add(new Objects.ObjXembly());
            }
            this.objects().start();
        }
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void exitBinding(final PhiParser.BindingContext ctx) {
        if (this.objs.size() > this.packages.size()) {
            if (ctx.alphaBinding() != null) {
                if (ctx.alphaBinding().attribute().VERTEX() != null) {
                    this.attributes.pop();
                    this.objs.removeLast();
                } else {
                    this.objects().enter()
                        .prop(this.properties.peek(), this.attributes.pop())
                        .leave();
                }
            } else if (ctx.emptyBinding() != null) {
                if (ctx.emptyBinding().attribute().VERTEX() != null) {
                    this.attributes.pop();
                    this.objs.removeLast();
                } else {
                    this.objects().enter()
                        .prop(this.properties.peek(), this.attributes.pop())
                        .leave();
                }
            }
        }
    }

    @Override
    public void enterAlphaBinding(final PhiParser.AlphaBindingContext ctx) {
        // Nothing here
    }

    @Override
    public void exitAlphaBinding(final PhiParser.AlphaBindingContext ctx) {
        // Nothing here
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void enterAttribute(final PhiParser.AttributeContext ctx) {
        final String attr;
        if (ctx.PHI() != null) {
            attr = "@";
        } else if (ctx.RHO() != null) {
            attr = "^";
        } else if (ctx.SIGMA() != null) {
            attr = "&";
        } else if (ctx.VERTEX() != null) {
            attr = "<";
        } else if (ctx.LABEL() != null) {
            attr = ctx.LABEL().getText();
        } else if (ctx.alphaAttr() != null) {
            attr = ctx.alphaAttr().INDEX().getText();
        } else {
            attr = "";
        }
        this.attributes.push(attr);
    }

    @Override
    public void exitAttribute(final PhiParser.AttributeContext ctx) {
        // Nothing here
    }

    @Override
    public void enterAlphaAttr(final PhiParser.AlphaAttrContext ctx) {
        // Nothing here
    }

    @Override
    public void exitAlphaAttr(final PhiParser.AlphaAttrContext ctx) {
        // Nothing here
    }

    @Override
    public void enterEmptyBinding(final PhiParser.EmptyBindingContext ctx) {
        // Nothing here
    }

    @Override
    public void exitEmptyBinding(final PhiParser.EmptyBindingContext ctx) {
        this.objects().leave();
    }

    @Override
    public void enterDeltaBidning(final PhiParser.DeltaBidningContext ctx) {
        if (ctx.EMPTY() != null) {
            throw new ParsingException(
                "It's impossible to represent Δ ⤍ ∅ binding in EO",
                new IllegalStateException(),
                ctx.getStart().getLine()
            );
        }
        this.objects()
            .start()
            .prop("data", "bytes")
            .prop("base", "org.eolang.bytes");
        if (!ctx.BYTES().getText().equals("--")) {
            this.objects().data(ctx.BYTES().getText().replaceAll("-", " ").trim());
        }
        this.objects().leave();
    }

    @Override
    public void exitDeltaBidning(final PhiParser.DeltaBidningContext ctx) {
        // Nothing here
    }

    @Override
    public void enterLambdaBidning(final PhiParser.LambdaBidningContext ctx) {
        if (!ctx.FUNCTION().getText().equals(XePhiListener.LAMBDA_PACKAGE)) {
            this.objects().prop("atom");
        }
    }

    @Override
    public void exitLambdaBidning(final PhiParser.LambdaBidningContext ctx) {
        // Nothing here
    }

    @Override
    public void enterApplication(final PhiParser.ApplicationContext ctx) {
        this.properties.push("as");
        this.objects().enter();
        if (ctx.bindings().binding().size() == 0) {
            this.objects().prop("copy");
        }
    }

    @Override
    public void exitApplication(final PhiParser.ApplicationContext ctx) {
        this.objects().leave();
        this.properties.pop();
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public void enterDispatch(final PhiParser.DispatchContext ctx) {
        this.objects().start().prop("method");
    }

    @Override
    public void exitDispatch(final PhiParser.DispatchContext ctx) {
        this.objects().prop("base", String.format(".%s", this.attributes.pop())).leave();
    }

    @Override
    public void enterApplicationsOrDispatches(final PhiParser.ApplicationsOrDispatchesContext ctx) {
        // Nothing here
    }

    @Override
    public void exitApplicationsOrDispatches(final PhiParser.ApplicationsOrDispatchesContext ctx) {
        // Nothing here
    }

    @Override
    public void enterTermination(final PhiParser.TerminationContext ctx) {
        // Nothing here
    }

    @Override
    public void exitTermination(final PhiParser.TerminationContext ctx) {
        // Nothing here
    }

    @Override
    public void visitTerminal(final TerminalNode node) {
        // Nothing here
    }

    @Override
    public void visitErrorNode(final ErrorNode node) {
        // Nothing here
    }

    @Override
    public void enterEveryRule(final ParserRuleContext ctx) {
        // Nothing here
    }

    @Override
    public void exitEveryRule(final ParserRuleContext ctx) {
        // Nothing here
    }

    @Override
    public Iterator<Directive> iterator() {
        return this.dirs.iterator();
    }

    /**
     * Objects at the last level of stack.
     * @return Objects
     */
    private Objects objects() {
        return this.objs.getLast();
    }

    /**
     * Check if bindings on the given context have lambda package.
     * @param ctx Context
     * @return If bindings have lambda package
     */
    private static boolean hasLambdaPackage(final PhiParser.BindingsContext ctx) {
        return ctx.binding()
            .stream()
            .anyMatch(
                context -> context.lambdaBidning() != null && context.lambdaBidning()
                    .FUNCTION()
                    .getText()
                    .equals(XePhiListener.LAMBDA_PACKAGE)
            );
    }
}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2022 Objectionary.com
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
package org.eolang.maven;

import com.jcabi.log.Logger;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.yegor256.tojos.Tojo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cactoos.iterable.Filtered;
import org.cactoos.list.ListOf;
import org.eolang.maven.hash.ChNarrow;
import org.eolang.maven.hash.ChResolve;
import org.eolang.maven.hash.CommitHash;
import org.eolang.maven.objectionary.Objectionary;
import org.eolang.maven.objectionary.OyFallbackSwap;
import org.eolang.maven.objectionary.OyHome;
import org.eolang.maven.objectionary.OyRemote;
import org.eolang.maven.util.Online;
import org.eolang.maven.util.Rel;

/**
 * Go through all `probe` metas in XMIR files, try to locate the
 * objects pointed by `probe` in Objectionary and if found register them in
 * catalog.
 *
 * @since 0.28.11
 * @checkstyle CyclomaticComplexityCheck (300 lines)
 */
@Mojo(
    name = "probe",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true
)
public final class ProbeMojo extends SafeMojo {

    /**
     * The Git hash to pull objects from, in objectionary.
     *
     * @since 0.21.0
     */
    @SuppressWarnings("PMD.ImmutableField")
    @Parameter(property = "eo.tag", required = true, defaultValue = "master")
    private String tag = "master";

    /**
     * Target directory.
     *
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(property = "eo.home")
    @SuppressWarnings("PMD.ImmutableField")
    private Path outputPath = Paths.get(System.getProperty("user.home")).resolve(".eo");

    /**
     * Read hashes from local file.
     *
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(property = "offlineHashFile")
    private Path offlineHashFile;

    /**
     * Return hash by pattern.
     * -DofflineHash=0.*.*:abc2sd3
     * -DofflineHash=0.2.7:abc2sd3,0.2.8:s4se2fe
     *
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(property = "offlineHash")
    private String offlineHash;

    /**
     * The objectionary.
     */
    @SuppressWarnings("PMD.ImmutableField")
    private Objectionary objectionary;

    @Override
    public void exec() throws IOException {
        if (!new Online().value()) {
            Logger.warn(
                this, "We are offline: skipping probes"
            );
            return;
        }
        final Collection<Tojo> tojos = this.scopedTojos().select(
            row -> row.exists(AssembleMojo.ATTR_XMIR2)
                && !row.exists(AssembleMojo.ATTR_PROBED)
        );
        final CommitHash hash = new ChResolve(
            this.offlineHashFile, this.offlineHash, this.tag
        );
        if (this.objectionary == null) {
            this.objectionary = new OyFallbackSwap(
                new OyHome(
                    new ChNarrow(hash),
                    this.outputPath
                ),
                new OyRemote(hash),
                this.forceUpdate()
            );
        }
        final Collection<String> probed = new HashSet<>(1);
        for (final Tojo tojo : tojos) {
            final Path src = Paths.get(tojo.get(AssembleMojo.ATTR_XMIR2));
            final Collection<String> names = this.probes(src);
            int count = 0;
            for (final String name : names) {
                if (!this.objectionary.contains(name)) {
                    continue;
                }
                ++count;
                final Tojo ftojo = this.scopedTojos().add(name);
                if (!ftojo.exists(AssembleMojo.ATTR_VERSION)) {
                    ftojo.set(AssembleMojo.ATTR_VERSION, "*.*.*");
                }
                ftojo.set(AssembleMojo.ATTR_DISCOVERED_AT, src);
                probed.add(name);
            }
            tojo.set(AssembleMojo.ATTR_HASH, new ChNarrow(hash).value());
            tojo.set(AssembleMojo.ATTR_PROBED, Integer.toString(count));
        }
        if (tojos.isEmpty()) {
            if (this.scopedTojos().select(row -> true).isEmpty()) {
                Logger.warn(this, "Nothing to probe, since there are no programs");
            } else {
                Logger.info(this, "Nothing to probe, all programs checked already");
            }
        } else if (probed.isEmpty()) {
            Logger.info(
                this, "No probes found in %d programs",
                tojos.size()
            );
        } else {
            Logger.info(
                this, "Found %d probes in %d programs: %s",
                probed.size(), tojos.size(), probed
            );
        }
    }

    /**
     * Find all probes found in the provided XML file.
     *
     * @param file The .xmir file
     * @return List of foreign objects found
     * @throws FileNotFoundException If not found
     * @todo #1395:30min Rewrite lines 187-204 as fully `cactoos` style to
     *  make to code more convenient. So there is will no "imperative" `forEach()`
     *  in line 194.
     */
    private Collection<String> probes(final Path file)
        throws FileNotFoundException {
        final XML xml = new XMLDocument(file);
        final Collection<String> ret = new HashSet<>(1);
        new TreeSet<>(
            new ListOf<>(
                new Filtered<>(
                    obj -> !obj.isEmpty(),
                    xml.xpath("//metas/meta[head/text() = 'probe']/tail/text()")
                )
            )
        ).forEach(
            obj -> {
                if (!ProbeMojo.hasReservedChars(obj)) {
                    if (obj.length() > 1 && "Q.".equals(obj.substring(0, 2))) {
                        ret.add(obj.substring(2));
                    } else {
                        ret.add(obj);
                    }
                }
            }
        );
        if (ret.isEmpty()) {
            Logger.debug(
                this, "Didn't find any probed objects in %s",
                new Rel(file)
            );
        } else {
            Logger.debug(
                this, "Found %d probed objects in %s: %s",
                ret.size(), new Rel(file), ret
            );
        }
        return ret;
    }

    /**
     * Checks if String has reserved symbols.
     *
     * @param str String
     * @return True if found
     * @todo #1395:30min Need to add the logic of "hasReservedChars" method to
     *  add-probes.xsl". After that, the method in this class need to be removed.
     * @checkstyle BooleanExpressionComplexityCheck (15 lines)
     */
    private static boolean hasReservedChars(final String str) {
        return str.contains("<")
            || str.contains(">")
            || str.contains("$")
            || str.contains("*")
            || str.contains("?")
            || str.contains(":")
            || str.contains("\"")
            || str.contains("|")
            || str.contains("^")
            || str.contains("@");
    }

    /**
     * Is force update option enabled.
     *
     * @return True if option enabled and false otherwise
     */
    private boolean forceUpdate() {
        return this.session.getRequest().isUpdateSnapshots();
    }

}

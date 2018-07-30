/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.jvnet.maven.plugin.antrun;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * &lt;jar> task extended to correctly merge manifest metadata.
 *
 * TODO: this contains HK2 knowledge, so it should be moved to HK2.
 *
 * @author Kohsuke Kawaguchi
 */
public class RejarTask extends Jar {
//
// these fields only have a life-span within the execute method.
//
    /**
     * Merged metadata files in <tt>META-INF</tt>
     */
    private final Map<String,ByteArrayOutputStream> metadata = new HashMap<String,ByteArrayOutputStream>();

    public void execute() throws BuildException {
        // we want to put metadata files earlier in the file for faster runtime access,
        // and for that we require two passes.
        doubleFilePass = true;

        super.execute();
    }

    protected void initZipOutputStream(ZipOutputStream zOut) throws IOException, BuildException {
        if (!skipWriting) {
            // write out the merged metadata and service entries
            for (Map.Entry<String,ByteArrayOutputStream> e : metadata.entrySet()) {
                super.zipFile(
                    new ByteArrayInputStream(e.getValue().toByteArray()),
                    zOut, e.getKey(),
                    System.currentTimeMillis(), null,
                    ZipFileSet.DEFAULT_FILE_MODE);
            }
        }

        super.initZipOutputStream(zOut);
    }

    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath, long lastModified, File fromArchive, int mode) throws IOException {
        boolean isInhabitantsFile = vPath.startsWith("META-INF/inhabitants/") || vPath.startsWith("META-INF/hk2-locator/");
        boolean isServicesFile = vPath.startsWith("META-INF/services/");

        if (isInhabitantsFile || isServicesFile)  {
            // merging happens in the first pass.
            // in the second pass, ignore them.
            if(skipWriting) {
                ByteArrayOutputStream stream = metadata.get(vPath);
                if (isServicesFile) {
                    if (stream != null)
                        stream.write(("\n").getBytes());
                }
                if(stream==null)
                    metadata.put(vPath,stream= new ByteArrayOutputStream());
                if(isInhabitantsFile) {
                    // print where the lines came from
                    stream.write(("# from "+fromArchive.getName()+"\n").getBytes());
                }
                IOUtils.copy(is,stream);
            }
            return;
        }

        // merge inhabitants file
        super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode);
    }
}

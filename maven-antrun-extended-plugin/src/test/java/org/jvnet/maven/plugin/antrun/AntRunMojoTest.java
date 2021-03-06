/*
 * Copyright (c) 2006-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jvnet.maven.plugin.antrun;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.StringOutputStream;

/**
 * Class to test AntRun plugin
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: AntRunMojoTest.java 511210 2007-02-24 04:52:20Z brett $
 */
public class AntRunMojoTest
    extends PlexusTestCase
{
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        // nop
    }

    /**
     * Method to test Default Antrun generation
     *
     * @throws Exception
     */
    public void testDefaultProject()
        throws Exception
    {
        try {
            String result = invokeMaven( "antrun-default-test", new Properties() );
            assertTrue( result.indexOf( "[echo] Hello World!" ) != -1 );
        } catch (LifecycleExecutionException ex) {
            String message = ex.getMessage();
            if (message.contains("'org.jvnet.maven-antrun-extended-plugin:maven-antrun-extended-plugin' not found in repository")) {
                System.out.println("maven-antrun-extended-plugin missing from local repository. Run mvn install and retest");
            }
            else {
                throw ex;
            }
        }
    }

    /**
     * Method to test tasks attributes
     *
     * @throws Exception
     */
    public void testTasksAttributesProject()
        throws Exception
    {
        try {
            Properties properties = new Properties();

            String result = invokeMaven( "tasksattributes-test", properties );
            assertTrue( result.indexOf( "[echo] To skip me" ) != -1 );

            properties.put( "maven.test.skip", "true" );
            result = invokeMaven( "tasksattributes-test", properties );
            assertTrue( result.indexOf( "[echo] To skip me" ) == -1 );
        } catch (LifecycleExecutionException ex) {
            String message = ex.getMessage();
            if (message.contains("'org.jvnet.maven-antrun-extended-plugin:maven-antrun-extended-plugin' not found in repository")) {
                System.out.println("maven-antrun-extended-plugin missing from local repository. Run mvn install and retest");
            }
            else {
                throw ex;
            }
        }
    }

    /**
     * Invoke Maven for a given test project name
     * <br/>
     * The Maven test project should be in a directory called <code>testProject</code> in
     * "src/test/resources/unit/" directory.
     * The Maven test project should be called <code>"testProject"-plugin-config.xml</code>.
     *
     * @param testProject
     * @param properties
     * @return the output of MavenEmbedder
     * @throws Exception
     */
    private String invokeMaven( String testProject, Properties properties )
        throws Exception
    {
        MavenEmbedder maven = new MavenEmbedder();
        maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        maven.setLogger( new MavenEmbedderConsoleLogger() );
        maven.setLocalRepositoryDirectory( getTestFile( "target/local-repo" ) );
        maven.setOffline( true );
        maven.start();

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        File testPom = new File( getBasedir(), "src/test/resources/unit/" + testProject + "/" + testProject
            + "-plugin-config.xml" );
        MavenProject project = maven.readProjectWithDependencies( testPom );

        PrintStream oldOut = System.out;
        OutputStream outOS = new StringOutputStream();
        PrintStream out = new PrintStream( outOS );
        System.setOut( out );

        try
        {
            maven.execute( project,
                           Arrays.asList( new String[] { "org.jvnet.maven-antrun-extended-plugin:maven-antrun-extended-plugin:run" } ),
                           eventMonitor, new ConsoleDownloadMonitor(), properties, new File( PlexusTestCase
                               .getBasedir(), "/target/test/unit/" + testProject + "/" ) );

            return outOS.toString();
        } 
        catch (LifecycleExecutionException ex) {
            // if maven-antrun-extended-plugin is not in target/local-repo and not in remote repos, look in default 
            // local repo location: ${user.home}/.m2/repository
            maven.setLocalRepositoryDirectory(null);
            maven.start();
            maven.execute( project,
                           Arrays.asList( new String[] { "org.jvnet.maven-antrun-extended-plugin:maven-antrun-extended-plugin:run" } ),
                           eventMonitor, new ConsoleDownloadMonitor(), properties, new File( PlexusTestCase
                               .getBasedir(), "/target/test/unit/" + testProject + "/" ) );
            return outOS.toString();
        }
        finally
        {
            System.setOut( oldOut );
        }
    }
    
    public static void main(String[] args) throws Exception {
        AntRunMojoTest test = new AntRunMojoTest();
        test.setUp();
        test.testDefaultProject();
        test.tearDown();
    }
}

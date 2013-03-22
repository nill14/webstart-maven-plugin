package org.codehaus.mojo.webstart.sign;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerException;
import org.apache.maven.shared.jarsigner.JarSignerRequest;
import org.apache.maven.shared.jarsigner.JarSignerResult;
import org.apache.maven.shared.jarsigner.JarSignerVerifyRequest;
import org.codehaus.mojo.keytool.KeyTool;
import org.codehaus.mojo.keytool.KeyToolException;
import org.codehaus.mojo.keytool.KeyToolResult;
import org.codehaus.mojo.keytool.requests.KeyToolGenerateKeyPairRequest;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation of the {@link SignTool}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="default"
 * @since 1.0-beta-3
 */
public class DefaultSignTool
    extends AbstractLogEnabled
    implements SignTool
{

    /**
     * All extension parts involved in a signature file.
     */
    private static final String[] EXT_ARRAY = { "DSA", "RSA", "SF" };

    /**
     * The component to invoke jarsigner command.
     *
     * @plexus.requirement role="org.apache.maven.shared.jarsigner.JarSigner"
     */
    private JarSigner jarSigner;

    /**
     * The component to invoke keyTool command.
     *
     * @plexus.requirement role="org.codehaus.mojo.keytool.KeyTool"
     */
    private KeyTool keyTool;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @plexus.requirement role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    /**
     * Filter to keep only files which extension contains one on the {@link #EXT_ARRAY}.
     */
    private FileFilter removeSignatureFileFilter = new FileFilter()
    {
        private final List extToRemove = Arrays.asList( EXT_ARRAY );

        public boolean accept( File file )
        {
            String extension = FileUtils.getExtension( file.getAbsolutePath() );
            return extToRemove.contains( extension );
        }
    };

    public void generateKey( SignConfig config, File keystoreFile )
        throws MojoExecutionException
    {
        KeyToolGenerateKeyPairRequest request = config.createKeyGenRequest( keystoreFile );

        try
        {
            KeyToolResult result = keyTool.execute( request );

            CommandLineException exception = result.getExecutionException();
            if ( exception != null )
            {
                throw new MojoExecutionException( "Could not sign jar " + keystoreFile, exception );
            }
        }
        catch ( KeyToolException e )
        {
            throw new MojoExecutionException( "Could not find keytool", e );
        }
    }

    public void sign( SignConfig config, File jarFile, File signedJar )
        throws MojoExecutionException
    {

        JarSignerRequest request = config.createSignRequest( jarFile, signedJar );

        try
        {
            JarSignerResult result = jarSigner.execute( request );

            CommandLineException exception = result.getExecutionException();
            if ( exception != null )
            {
                throw new MojoExecutionException( "Could not sign jar " + jarFile, exception );
            }
        }
        catch ( JarSignerException e )
        {
            throw new MojoExecutionException( "Could not find jarSigner", e );
        }
    }

    public void verify( SignConfig config, File jarFile, boolean certs )
        throws MojoExecutionException
    {

        JarSignerRequest request = config.createVerifyRequest( jarFile, certs );

        try
        {
            JarSignerResult result = jarSigner.execute( request );

            CommandLineException exception = result.getExecutionException();
            if ( exception != null )
            {
                throw new MojoExecutionException( "Could not verify jar " + jarFile, exception );
            }
        }
        catch ( JarSignerException e )
        {
            throw new MojoExecutionException( "Could not find jarSigner", e );
        }
    }

    public boolean isJarSigned( SignConfig config, File jarFile )
        throws MojoExecutionException
    {
        JarSignerVerifyRequest request = config.createVerifyRequest( jarFile, false );

        LineMatcherStreamConsumer consumer = new LineMatcherStreamConsumer( config.isVerbose(), "jar verified." );
        request.setSystemOutStreamConsumer( consumer );
        try
        {
            JarSignerResult result = jarSigner.execute( request );

            CommandLineException exception = result.getExecutionException();
            if ( exception != null )
            {
                throw new MojoExecutionException( "Could not verify jar " + jarFile, exception );
            }

            return consumer.matched;
        }
        catch ( JarSignerException e )
        {
            throw new MojoExecutionException( "Could not find jarSigner", e );
        }
    }

    public void unsign( File jarFile, File tempDirectory, boolean verbose )
        throws MojoExecutionException
    {

        String archiveExt = FileUtils.getExtension( jarFile.getAbsolutePath() ).toLowerCase();

        // create temp dir
        File tempDir = new File( tempDirectory, jarFile.getName() );

        if ( !tempDir.mkdirs() )
        {
            throw new MojoExecutionException( "Error creating temporary directory: " + tempDir );
        }
        // FIXME we probably want to be more security conservative here.
        // it's very easy to guess where the directory will be and possible
        // to access/change its contents before the file is rejared..

        // extract jar into temporary directory
        try
        {
            UnArchiver unArchiver = this.archiverManager.getUnArchiver( archiveExt );
            unArchiver.setSourceFile( jarFile );
            unArchiver.setDestDirectory( tempDir );
            unArchiver.extract();
        }
        catch ( ArchiverException ex )
        {
            throw new MojoExecutionException( "Error unpacking file: " + jarFile + "to: " + tempDir, ex );
        }
        catch ( NoSuchArchiverException ex )
        {
            throw new MojoExecutionException( "Error acquiring unarchiver for extension: " + archiveExt, ex );
        }

        // create and check META-INF directory
        File metaInf = new File( tempDir, "META-INF" );
        if ( !metaInf.isDirectory() )
        {
            verboseLog( verbose, "META-INF dir not found : nothing to do for file: " + jarFile.getAbsolutePath() );
            return;
        }

        // filter signature files and remove them
        File[] filesToRemove = metaInf.listFiles( this.removeSignatureFileFilter );
        if ( filesToRemove.length == 0 )
        {
            verboseLog( verbose, "no files match " + Arrays.asList( EXT_ARRAY ) + " : nothing to do for file: " +
                jarFile.getAbsolutePath() );
            return;
        }
        for ( int i = 0; i < filesToRemove.length; i++ )
        {
            if ( !filesToRemove[i].delete() )
            {
                throw new MojoExecutionException( "Error removing signature file: " + filesToRemove[i] );
            }
            verboseLog( verbose, "remove file :" + filesToRemove[i] );
        }

        // recreate archive
        try
        {
            JarArchiver jarArchiver = (JarArchiver) this.archiverManager.getArchiver( "jar" );
            jarArchiver.setUpdateMode( false );
            jarArchiver.addDirectory( tempDir );
            jarArchiver.setDestFile( jarFile );
            jarArchiver.createArchive();

        }
        catch ( ArchiverException ex )
        {
            throw new MojoExecutionException( "Error packing directory: " + tempDir + "to: " + jarFile, ex );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "Error packing directory: " + tempDir + "to: " + jarFile, ex );
        }
        catch ( NoSuchArchiverException ex )
        {
            throw new MojoExecutionException( "Error acquiring archiver for extension: jar", ex );
        }

        try
        {
            FileUtils.deleteDirectory( tempDir );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "Error cleaning up temporary directory file: " + tempDir, ex );
        }
    }

    public void deleteKeyStore( File keystore, boolean verbose )
    {
        if ( keystore.exists() )
        {
            if ( keystore.delete() )
            {
                infoOrDebug( verbose, "deleted keystore from: " + keystore.getAbsolutePath() );
            }
            else
            {
                getLogger().warn( "Couldn't delete keystore from: " + keystore.getAbsolutePath() );
            }
        }
        else
        {
            infoOrDebug( verbose, "Skipping deletion of non existing keystore: " + keystore.getAbsolutePath() );
        }
    }

    /**
     * Log as info when verbose or info is enabled, as debug otherwise.
     *
     * @param verbose verbose level
     * @param msg     message to log
     */
    protected void verboseLog( boolean verbose, String msg )
    {
        infoOrDebug( verbose || getLogger().isInfoEnabled(), msg );
    }

    /**
     * Log a message as info or debug.
     *
     * @param info if set to true, log as info(), otherwise as debug()
     * @param msg  message to log
     */
    private void infoOrDebug( boolean info, String msg )
    {
        if ( info )
        {
            getLogger().info( msg );
        }
        else
        {
            getLogger().debug( msg );
        }
    }

    // checks if a consumed line matches
    private class LineMatcherStreamConsumer
        implements StreamConsumer
    {
        private String toMatch;

        private boolean matched;

        private boolean verbose;

        LineMatcherStreamConsumer( boolean verbose, String toMatch )
        {
            this.toMatch = toMatch;
            this.verbose = verbose;
        }

        public void consumeLine( String line )
        {
            matched = matched || toMatch.equals( line );

            if ( verbose )
            {
                getLogger().info( line );
            }
            else
            {
                getLogger().debug( line );
            }
        }
    }

}
